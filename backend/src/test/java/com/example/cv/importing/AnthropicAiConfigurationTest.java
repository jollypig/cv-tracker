package com.example.cv.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatProperties;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AnthropicAiConfigurationTest {

    private final ChatModel chatModel = mock(ChatModel.class);
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory()
                .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(AnthropicAiConfiguration.class, OllamaCvAiExtractor.class)
            .withBean(ChatModel.class, () -> chatModel)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(ParsedCvOutputValidator.class, () -> new ParsedCvOutputValidator(
                    Validation.buildDefaultValidatorFactory().getValidator()))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
        void claudeDefaultsOmitDeprecatedTemperature() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yml")).get(0));
        AnthropicChatProperties properties = Binder.get(environment)
            .bind("spring.ai.anthropic.chat", AnthropicChatProperties.class).get();

        contextRunner.withPropertyValues("cv.ai.provider=anthropic", "spring.ai.anthropic.api-key=test-key")
            .withBean(AnthropicChatProperties.class, () -> properties)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(properties.getOptions().getTemperature()).isNull();
                RestClient.Builder restClientBuilder = RestClient.builder();
                MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
                server.expect(jsonPath("$.temperature").doesNotExist())
                    .andRespond(withSuccess("""
                        {"id":"test-message","type":"message","role":"assistant",
                        "model":"test-model","content":[{"type":"text","text":"{}"}],
                        "stop_reason":"end_turn","usage":{"input_tokens":1,"output_tokens":1}}
                        """, MediaType.APPLICATION_JSON));
                AnthropicApi api = AnthropicApi.builder().apiKey("test-key")
                    .restClientBuilder(restClientBuilder).build();
                AnthropicChatModel model = AnthropicChatModel.builder().anthropicApi(api)
                    .defaultOptions(properties.getOptions()).build();

                model.call(new Prompt("CV fixture"));

                server.verify();
            });
        }

        @Test
    void selectsClaudeAndParsesCvUsingConfiguredModelMetadata() {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(
                new AssistantMessage("""
                        {"personalData":null,"professionalSummary":null,"employment":[],
                        "projects":[],"education":[],"languages":[],"skills":[]}
                        """)))));

        contextRunner.withPropertyValues("cv.ai.provider=anthropic", "spring.ai.anthropic.api-key=test-key",
                "spring.ai.anthropic.chat.options.model=claude-sonnet-4-5", "cv.ai.model-version=test-version")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(CvAiExtractor.class);
                    CvAiExtractor extractor = context.getBean(CvAiExtractor.class);
                    assertThat(extractor.modelMetadata()).isEqualTo(
                            new CvAiExtractor.ModelMetadata("claude-sonnet-4-5", "test-version"));
                    ParsedCv parsed = extractor.extract(new NormalizedCvDocument(List.of()));
                    assertThat(parsed.skills()).isEmpty();
                    assertThat(parsed.employment()).isEmpty();
                });
    }

    @Test
    void preservesSafeFailureHandlingForClaude() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("provider unavailable"));

        contextRunner.withPropertyValues("cv.ai.provider=anthropic", "spring.ai.anthropic.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThatThrownBy(() -> context.getBean(CvAiExtractor.class)
                            .extract(new NormalizedCvDocument(List.of())))
                            .isInstanceOf(CvAiExtractionException.class)
                            .hasMessage("CV extraction failed; verify the configured AI provider and its response.");
                });
    }

    @Test
    void keepsOllamaAsDefaultWithoutAnthropicCredentials() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(CvAiExtractor.class);
            assertThat(context).doesNotHaveBean("anthropicCvAiExtractor");
            assertThat(context).doesNotHaveBean("anthropicApi");
        });
    }

    @Test
    void rejectsMissingClaudeCredentialsAtStartup() {
        contextRunner.withPropertyValues("cv.ai.provider=anthropic")
                .run(context -> assertThat(context).hasFailed());
    }
}