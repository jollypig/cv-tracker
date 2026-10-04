package com.example.cv.importing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "cv.ai", name = "provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaCvAiExtractor implements CvAiExtractor {

    private static final String EXTRACTION_INSTRUCTIONS = """
            Extract the CV data from the normalized document below.
            Use only information present in the document. Do not invent values.
            Use null for unknown scalar values and empty arrays for unknown collections.
            Return a value matching this schema:
            %s

            Normalized document:
            %s
            """;

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final BeanOutputConverter<ParsedCv> outputConverter = new BeanOutputConverter<>(ParsedCv.class);

    public OllamaCvAiExtractor(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public ParsedCv extract(NormalizedCvDocument document) {
        try {
            String normalizedDocument = objectMapper.writeValueAsString(document);
            String promptText = EXTRACTION_INSTRUCTIONS.formatted(outputConverter.getFormat(), normalizedDocument);
            ChatResponse response = chatModel.call(new Prompt(promptText));
            String output = response.getResult().getOutput().getText();
            ParsedCv parsedCv = outputConverter.convert(output);
            if (parsedCv == null) {
                throw new IllegalArgumentException("The model returned no CV data");
            }
            return parsedCv;
        } catch (JsonProcessingException | RuntimeException exception) {
            if (exception instanceof CvAiExtractionException extractionException) {
                throw extractionException;
            }
            throw new CvAiExtractionException(
                    "CV extraction failed; verify the configured AI provider and its response.",
                    exception
            );
        }
    }
}
