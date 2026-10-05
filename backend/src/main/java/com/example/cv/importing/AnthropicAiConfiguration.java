package com.example.cv.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "cv.ai", name = "provider", havingValue = "anthropic")
public class AnthropicAiConfiguration {

        @Bean
        static BeanPostProcessor anthropicTemperatureCompatibility() {
                return new BeanPostProcessor() {
                        @Override
                        public Object postProcessBeforeInitialization(Object bean, String beanName) {
                                if (bean instanceof AnthropicChatProperties properties) {
                                        properties.getOptions().setTemperature(null);
                                }
                                return bean;
                        }
                };
        }

    @Bean
    @ConditionalOnMissingBean(AnthropicApi.class)
    AnthropicApi anthropicApi(
            @Value("${spring.ai.anthropic.api-key:}") String apiKey,
            @Value("${spring.ai.anthropic.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${cv.ai.request-timeout:60s}") Duration requestTimeout
    ) {
                Assert.hasText(apiKey, "ANTHROPIC_API_KEY must be configured when CV_AI_PROVIDER=anthropic");
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(requestTimeout);
        requestFactory.setReadTimeout(requestTimeout);

        return AnthropicApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .webClientBuilder(WebClient.builder())
                .build();
    }

    @Bean
    CvAiExtractor anthropicCvAiExtractor(
            ChatModel chatModel,
            ObjectMapper objectMapper,
            ParsedCvOutputValidator outputValidator,
            MeterRegistry meterRegistry,
            @Value("${spring.ai.anthropic.chat.options.model:claude-sonnet-4-5}") String modelName,
            @Value("${cv.ai.model-version:unknown}") String modelVersion
    ) {
        return new OllamaCvAiExtractor(chatModel, objectMapper, outputValidator, meterRegistry,
                modelName, modelVersion);
    }
}