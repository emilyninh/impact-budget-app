package com.impactbudget.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which chat model powers the assistant, bound from {@code assistant.*}. Defaults to the free,
 * local {@code ollama} provider so the chat works with no API key.
 */
@ConfigurationProperties(prefix = "assistant")
public record AssistantProperties(String provider) {

    /** {@code ollama} (free/local, default) | {@code claude} (paid, higher quality). */
    public String providerOrDefault() {
        return (provider == null || provider.isBlank()) ? "ollama" : provider;
    }
}
