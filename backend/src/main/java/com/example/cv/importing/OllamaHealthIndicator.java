package com.example.cv.importing;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("ollama")
@ConditionalOnProperty(prefix = "cv.ai", name = "provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaHealthIndicator implements HealthIndicator {

    private final OllamaApi ollamaApi;

    public OllamaHealthIndicator(OllamaApi ollamaApi) {
        this.ollamaApi = ollamaApi;
    }

    @Override
    public Health health() {
        try {
            ollamaApi.listModels();
            return Health.up().withDetail("provider", "ollama").build();
        } catch (RuntimeException exception) {
            return Health.down().withDetail("provider", "ollama").withDetail("status", "unavailable").build();
        }
    }
}
