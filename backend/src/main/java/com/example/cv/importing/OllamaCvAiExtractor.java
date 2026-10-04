package com.example.cv.importing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "cv.ai", name = "provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaCvAiExtractor implements CvAiExtractor {

    private static final String SECTION_INSTRUCTIONS = """
            Identify the logical sections in this CV. Return JSON matching the schema below.
            Use canonical types PROFILE, EXPERIENCE, EDUCATION, PROJECTS, SKILLS, LANGUAGES,
            CERTIFICATIONS, or UNKNOWN. Map heading variations such as Work History and Career
            History to EXPERIENCE, About Me and Professional Summary to PROFILE, and Technical
            Skills to SKILLS. Keep unrecognized sections as UNKNOWN.
            start is the inclusive character offset and end is the exclusive character offset in
            the CV text below. Sections must be ordered, non-overlapping, and within the text.
            %s

            CV text:
            %s
            """;
    private static final String EXTRACTION_INSTRUCTIONS = """
            Extract the CV data from the normalized document below.
            Use only information present in the document. Do not invent values.
            Use null for unknown scalar values and empty arrays for unknown collections.
            For ambiguous skill names, you may suggest a canonical name only from this allowlist:
            %s
            Preserve each raw skill name. Never assign canonical IDs or calculate skill experience.
            Leave unmatched skills without a canonical suggestion so they remain available for review.
            Detected logical sections:
            %s

            Return a value matching this schema:
            %s

            Normalized document:
            %s
            """;

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final ParsedCvOutputValidator outputValidator;
    private final MeterRegistry meterRegistry;
    private final CvAiExtractor.ModelMetadata modelMetadata;
    private final BeanOutputConverter<DetectedCvSections> sectionOutputConverter =
            new BeanOutputConverter<>(DetectedCvSections.class);
    private final BeanOutputConverter<ParsedCv> outputConverter = new BeanOutputConverter<>(ParsedCv.class);

    public OllamaCvAiExtractor(
            ChatModel chatModel,
            ObjectMapper objectMapper,
            ParsedCvOutputValidator outputValidator,
            MeterRegistry meterRegistry,
            @Value("${spring.ai.ollama.chat.options.model:unknown}") String modelName,
            @Value("${cv.ai.model-version:unknown}") String modelVersion
    ) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.outputValidator = outputValidator;
        this.meterRegistry = meterRegistry;
        this.modelMetadata = new CvAiExtractor.ModelMetadata(modelName, modelVersion);
    }

    @Override
    public ModelMetadata modelMetadata() {
        return modelMetadata;
    }

    @Override
    public DetectedCvSections detectSections(NormalizedCvDocument document) {
        String text = normalizedText(document);
        if (text.isBlank()) {
            return new DetectedCvSections(List.of());
        }

        String promptText = SECTION_INSTRUCTIONS.formatted(sectionOutputConverter.getFormat(), text);
        String output;
        try {
            ChatResponse response = requestModel(new Prompt(promptText));
            output = response.getResult().getOutput().getText();
        } catch (RuntimeException exception) {
            return detectSectionsFromHeadings(document, text);
        }

        try {
            DetectedCvSections detected = sectionOutputConverter.convert(output);
            return validateSections(detected, document, text);
        } catch (RuntimeException exception) {
            return detectSectionsFromHeadings(document, text);
        }
    }

    @Override
    public ParsedCv extract(NormalizedCvDocument document) {
        try {
            DetectedCvSections sections = detectSections(document);
            String normalizedDocument = objectMapper.writeValueAsString(document);
            String normalizedSections = objectMapper.writeValueAsString(sections);
            String promptText = EXTRACTION_INSTRUCTIONS.formatted(
                    SkillCatalog.canonicalNames(),
                    normalizedSections,
                    outputConverter.getFormat(),
                    normalizedDocument
            );
            ChatResponse response = requestModel(new Prompt(promptText));
            String output = response.getResult().getOutput().getText();
            ParsedCv parsedCv = outputConverter.convert(output);
            if (parsedCv == null) {
                throw new IllegalArgumentException("The model returned no CV data");
            }
            return outputValidator.validate(CvPersonalDataEnricher.enrich(parsedCv, document));
        } catch (JsonProcessingException | RuntimeException exception) {
            if (exception instanceof CvAiExtractionException extractionException) {
                throw extractionException;
            }
            throw extractionFailure(exception);
        }
    }

    private DetectedCvSections validateSections(
            DetectedCvSections detected,
            NormalizedCvDocument document,
            String text
    ) {
        if (detected == null || detected.sections().isEmpty()) {
            return detectSectionsFromHeadings(document, text);
        }

        List<DetectedCvSection> validated = new ArrayList<>();
        int previousEnd = 0;
        for (DetectedCvSection section : detected.sections()) {
            if (section == null || section.type() == null || section.type().isBlank()
                    || section.start() < previousEnd || section.end() <= section.start()
                    || section.end() > text.length()) {
                return detectSectionsFromHeadings(document, text);
            }
            CvSectionType type = section.sectionType();
            validated.add(new DetectedCvSection(type.name(), section.start(), section.end()));
            previousEnd = section.end();
        }
        return new DetectedCvSections(validated);
    }

    private DetectedCvSections detectSectionsFromHeadings(NormalizedCvDocument document, String text) {
        List<Integer> starts = new ArrayList<>();
        List<CvSectionType> types = new ArrayList<>();
        int offset = 0;
        for (NormalizedCvBlock block : document.blocks()) {
            if (block.type() == NormalizedCvBlock.Type.HEADING) {
                starts.add(offset);
                types.add(CvSectionType.fromName(block.text()));
            }
            offset += block.text().length() + 1;
        }

        if (starts.isEmpty()) {
            return new DetectedCvSections(List.of(new DetectedCvSection(CvSectionType.UNKNOWN.name(), 0, text.length())));
        }

        List<DetectedCvSection> sections = new ArrayList<>();
        for (int index = 0; index < starts.size(); index++) {
            int end = index + 1 < starts.size() ? starts.get(index + 1) : text.length();
            if (end > starts.get(index)) {
                sections.add(new DetectedCvSection(types.get(index).name(), starts.get(index), end));
            }
        }
        return new DetectedCvSections(sections);
    }

    private String normalizedText(NormalizedCvDocument document) {
        return document.blocks().stream()
                .map(NormalizedCvBlock::text)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private CvAiExtractionException extractionFailure(Exception exception) {
        if (exception instanceof CvAiExtractionException extractionException) {
            return extractionException;
        }
        return new CvAiExtractionException(
                "CV extraction failed; verify the configured AI provider and its response.",
                exception
        );
    }

    private ChatResponse requestModel(Prompt prompt) {
        Timer.Sample requestTimer = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            return chatModel.call(prompt);
        } catch (RuntimeException exception) {
            outcome = "failure";
            Counter.builder("cv.ai.request.failures").register(meterRegistry).increment();
            throw exception;
        } finally {
            requestTimer.stop(Timer.builder("cv.ai.request")
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        }
    }
}
