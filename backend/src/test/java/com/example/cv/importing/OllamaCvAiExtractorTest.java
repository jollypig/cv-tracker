package com.example.cv.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import static org.mockito.Mockito.doThrow;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class OllamaCvAiExtractorTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String PARSED_CV_JSON = """
            {"personalData":null,"professionalSummary":null,"employment":[],"projects":[],"education":[],"languages":[],"skills":[]}
            """;

        private static final String SECTIONS_JSON = """
            {"sections":[{"type":"WORK HISTORY","start":0,"end":25}]}
            """;

    @Test
    void convertsModelResponseToParsedCv() {
        ChatModel chatModel = mock(ChatModel.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage message = mock(AssistantMessage.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
        when(message.getText()).thenReturn(PARSED_CV_JSON);

        OllamaCvAiExtractor extractor = extractor(chatModel, meterRegistry);
        ParsedCv result = extractor.extract(new NormalizedCvDocument(List.of()));

        assertThat(result).isNotNull();
        assertThat(result.employment()).isEmpty();
        assertThat(result.skills()).isEmpty();
        assertThat(meterRegistry.get("cv.ai.request").tag("outcome", "success").timer().count()).isEqualTo(1);
    }

    @Test
    void reportsProviderFailureWithoutIncludingCvContent() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("connection refused"));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        OllamaCvAiExtractor extractor = extractor(chatModel, meterRegistry);

        assertThatThrownBy(() -> extractor.extract(new NormalizedCvDocument(List.of())))
                .isInstanceOf(CvAiExtractionException.class)
                .hasMessage("CV extraction failed; verify the configured AI provider and its response.")
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(meterRegistry.get("cv.ai.request").tag("outcome", "failure").timer().count()).isEqualTo(1);
        assertThat(meterRegistry.get("cv.ai.request.failures").counter().count()).isEqualTo(1);
    }

    @Test
    void exposesConfiguredModelIdentityAsTechnicalMetadata() {
        OllamaCvAiExtractor extractor = new OllamaCvAiExtractor(mock(ChatModel.class), new ObjectMapper(),
                new ParsedCvOutputValidator(VALIDATOR), new SimpleMeterRegistry(), "llama3.2", "3b-q4");

        assertThat(extractor.modelMetadata()).isEqualTo(new CvAiExtractor.ModelMetadata("llama3.2", "3b-q4"));
    }

    @Test
    void detectsSectionsAndNormalizesCommonHeadingVariations() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage message = mock(AssistantMessage.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
        when(message.getText()).thenReturn(SECTIONS_JSON);
        NormalizedCvDocument document = new NormalizedCvDocument(List.of(
                new NormalizedCvBlock(NormalizedCvBlock.Type.TEXT, "Work History and projects", null)
        ));

        DetectedCvSections result = extractor(chatModel).detectSections(document);

        assertThat(result.sections()).containsExactly(new DetectedCvSection("EXPERIENCE", 0, 25));
    }

    @Test
    void fallsBackToUnknownSectionWhenModelCallFails() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("connection refused"));
        NormalizedCvDocument document = new NormalizedCvDocument(List.of(
                new NormalizedCvBlock(NormalizedCvBlock.Type.TEXT, "Unlabeled CV content", null)
        ));

        DetectedCvSections result = extractor(chatModel).detectSections(document);

        assertThat(result.sections()).containsExactly(new DetectedCvSection("UNKNOWN", 0, 20));
    }

    @Test
    void detectsSectionsBeforeExtractingCvData() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse sectionResponse = mock(ChatResponse.class);
        Generation sectionGeneration = mock(Generation.class);
        AssistantMessage sectionMessage = mock(AssistantMessage.class);
        ChatResponse extractionResponse = mock(ChatResponse.class);
        Generation extractionGeneration = mock(Generation.class);
        AssistantMessage extractionMessage = mock(AssistantMessage.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(sectionResponse, extractionResponse);
        when(sectionResponse.getResult()).thenReturn(sectionGeneration);
        when(sectionGeneration.getOutput()).thenReturn(sectionMessage);
        when(sectionMessage.getText()).thenReturn(SECTIONS_JSON);
        when(extractionResponse.getResult()).thenReturn(extractionGeneration);
        when(extractionGeneration.getOutput()).thenReturn(extractionMessage);
        when(extractionMessage.getText()).thenReturn(PARSED_CV_JSON);
        NormalizedCvDocument document = new NormalizedCvDocument(List.of(
                new NormalizedCvBlock(NormalizedCvBlock.Type.TEXT, "Work History and projects", null)
        ));

        ParsedCv result = extractor(chatModel).extract(document);

        assertThat(result).isNotNull();
        org.mockito.Mockito.verify(chatModel, org.mockito.Mockito.times(2)).call(any(Prompt.class));
    }

    @Test
    void extractionPromptDoesNotTreatJobTitlesAsSkillsAndSpecifiesSkillShape() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage message = mock(AssistantMessage.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
        when(message.getText()).thenReturn(PARSED_CV_JSON);
        NormalizedCvDocument document = new NormalizedCvDocument(List.of(
                new NormalizedCvBlock(NormalizedCvBlock.Type.TEXT, "Test1 User\nTest Position\nCity", null)
        ));

        extractor(chatModel).extract(document);

        org.mockito.ArgumentCaptor<Prompt> promptCaptor = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        org.mockito.Mockito.verify(chatModel, atLeastOnce()).call(promptCaptor.capture());
        String extractionPrompt = promptCaptor.getAllValues().get(1).getInstructions().get(0).getText();
        assertThat(extractionPrompt).contains("Do not treat a person's name, job title, employer, or location as a skill")
                .contains("name must be an object with value, confidence, and sourceText; evidence must be an array")
                .contains("Do not repeat JSON property names");
    }

        @Test
        void extractsContactDetailsDeterministicallyAndRejectsUnsupportedPersonalData() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse sectionResponse = mock(ChatResponse.class);
        Generation sectionGeneration = mock(Generation.class);
        AssistantMessage sectionMessage = mock(AssistantMessage.class);
        ChatResponse extractionResponse = mock(ChatResponse.class);
        Generation extractionGeneration = mock(Generation.class);
        AssistantMessage extractionMessage = mock(AssistantMessage.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(sectionResponse, extractionResponse);
        when(sectionResponse.getResult()).thenReturn(sectionGeneration);
        when(sectionGeneration.getOutput()).thenReturn(sectionMessage);
        when(sectionMessage.getText()).thenReturn("not json");
        when(extractionResponse.getResult()).thenReturn(extractionGeneration);
        when(extractionGeneration.getOutput()).thenReturn(extractionMessage);
        when(extractionMessage.getText()).thenReturn("""
            {"personalData":{"firstName":{"value":"Invented","confidence":1.0,"sourceText":"Alice Example"},
            "lastName":null,"email":{"value":"invented@example.com","confidence":1.0,"sourceText":"Alice Example"},
            "phone":null,"location":null,"urls":[]},"professionalSummary":null,"employment":[],
            "projects":[],"education":[],"languages":[],"skills":[]}
            """);
        NormalizedCvDocument document = new NormalizedCvDocument(List.of(
            new NormalizedCvBlock(NormalizedCvBlock.Type.TEXT,
                "Alice Example\nEmail: alice@example.com\nEmployment: 2020-2024\nPhone: +1 (555) 123-4567", null),
            new NormalizedCvBlock(NormalizedCvBlock.Type.LINK, "Portfolio", null, "https://portfolio.example")
        ));

        ParsedCv extracted = extractor(chatModel).extract(document);
        PersonalData result = extracted.personalData();

        assertThat(result.firstName()).isNull();
        assertThat(result.email()).isEqualTo(new ExtractedValue<>("alice@example.com", 1.0, "alice@example.com"));
        assertThat(result.phone()).isEqualTo(new ExtractedValue<>("+1 (555) 123-4567", 0.95,
            "+1 (555) 123-4567"));
        assertThat(result.urls()).containsExactly(new ExtractedValue<>("https://portfolio.example", 1.0, "Portfolio"));
        assertThat(extracted.warnings()).anyMatch(warning -> warning.contains("first name requires review"));
        }

    @Test
    void fallsBackToHeadingsWhenModelReturnsInvalidSectionRanges() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage message = mock(AssistantMessage.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
        when(message.getText()).thenReturn("{\"sections\":[{\"type\":\"SKILLS\",\"start\":4,\"end\":2}]}");
        NormalizedCvDocument document = new NormalizedCvDocument(List.of(
                new NormalizedCvBlock(NormalizedCvBlock.Type.HEADING, "Technical Skills", null),
                new NormalizedCvBlock(NormalizedCvBlock.Type.LIST_ITEM, "Java", null)
        ));

        DetectedCvSections result = extractor(chatModel).detectSections(document);

        assertThat(result.sections()).containsExactly(new DetectedCvSection("SKILLS", 0, 21));
    }

    @Test
    void preservesUnknownSectionsAsUnknown() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage message = mock(AssistantMessage.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
        when(message.getText()).thenReturn("{\"sections\":[{\"type\":\"Hobbies\",\"start\":0,\"end\":6}]}");
        NormalizedCvDocument document = new NormalizedCvDocument(List.of(
                new NormalizedCvBlock(NormalizedCvBlock.Type.TEXT, "Hobbies", null)
        ));

        DetectedCvSections result = extractor(chatModel).detectSections(document);

        assertThat(result.sections()).containsExactly(new DetectedCvSection("UNKNOWN", 0, 6));
    }

    @Test
    void healthIsUpWhenOllamaResponds() {
        OllamaApi ollamaApi = mock(OllamaApi.class);
        OllamaHealthIndicator healthIndicator = new OllamaHealthIndicator(ollamaApi);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void healthIsDownWhenOllamaIsUnavailable() {
        OllamaApi ollamaApi = mock(OllamaApi.class);
        doThrow(new IllegalStateException("connection refused")).when(ollamaApi).listModels();
        OllamaHealthIndicator healthIndicator = new OllamaHealthIndicator(ollamaApi);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("status", "unavailable");
    }

    @Test
    void returnsPartialDataWithActionableWarningForInvalidConfidence() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage message = mock(AssistantMessage.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
        when(message.getText()).thenReturn(PARSED_CV_JSON.replace("\"professionalSummary\":null",
                "\"professionalSummary\":{\"value\":\"Engineer\",\"confidence\":1.5,\"sourceText\":\"Engineer\"}"));

        ParsedCv result = extractor(chatModel).extract(new NormalizedCvDocument(List.of()));

        assertThat(result.professionalSummary().value()).isEqualTo("Engineer");
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("professionalSummary.confidence")
                && warning.contains("less than or equal to 1.0"));
    }

    private OllamaCvAiExtractor extractor(ChatModel chatModel) {
        return extractor(chatModel, new SimpleMeterRegistry());
    }

    private OllamaCvAiExtractor extractor(ChatModel chatModel, SimpleMeterRegistry meterRegistry) {
        return new OllamaCvAiExtractor(chatModel, new ObjectMapper(), new ParsedCvOutputValidator(VALIDATOR),
                meterRegistry, "test-model", "test-version");
    }
}
