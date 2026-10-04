package com.example.cv.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
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

class OllamaCvAiExtractorTest {

    private static final String PARSED_CV_JSON = """
            {"personalData":null,"professionalSummary":null,"employment":[],"projects":[],"education":[],"languages":[],"skills":[]}
            """;

    @Test
    void convertsModelResponseToParsedCv() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage message = mock(AssistantMessage.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
        when(message.getText()).thenReturn(PARSED_CV_JSON);

        OllamaCvAiExtractor extractor = new OllamaCvAiExtractor(chatModel, new ObjectMapper());
        ParsedCv result = extractor.extract(new NormalizedCvDocument(List.of()));

        assertThat(result).isNotNull();
        assertThat(result.employment()).isEmpty();
        assertThat(result.skills()).isEmpty();
    }

    @Test
    void reportsProviderFailureWithoutIncludingCvContent() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("connection refused"));
        OllamaCvAiExtractor extractor = new OllamaCvAiExtractor(chatModel, new ObjectMapper());

        assertThatThrownBy(() -> extractor.extract(new NormalizedCvDocument(List.of())))
                .isInstanceOf(CvAiExtractionException.class)
                .hasMessage("CV extraction failed; verify the configured AI provider and its response.")
                .hasCauseInstanceOf(IllegalStateException.class);
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
}
