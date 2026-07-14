package com.impactbudget.categorization;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Selects which merchant scorer fills the cache on a miss.
 *
 * @param provider {@code ollama} (local, free — default), {@code claude} (API key), or
 *                 {@code none} (neutral heuristic only)
 */
@ConfigurationProperties(prefix = "categorization.scoring")
public record ScoringProperties(String provider) {

    public String providerOrDefault() {
        return (provider == null || provider.isBlank()) ? "ollama" : provider;
    }
}
