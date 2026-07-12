package com.impactbudget.categorization;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Anthropic API configuration, bound from {@code anthropic.*}. When {@code apiKey} is
 * blank the categorization engine falls back to a neutral heuristic so the app still runs.
 */
@ConfigurationProperties(prefix = "anthropic")
public record AnthropicProperties(String apiKey, String model) {

    public String modelOrDefault() {
        return (model == null || model.isBlank()) ? "claude-opus-4-8" : model;
    }
}
