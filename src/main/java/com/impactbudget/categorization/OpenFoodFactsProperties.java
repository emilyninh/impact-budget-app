package com.impactbudget.categorization;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config for the free, key-less <a href="https://world.openfoodfacts.org">Open Food Facts</a>
 * enrichment step, which overlays a real eco-score on the sustainability dimension when a
 * merchant/brand matches food or packaged-goods products.
 */
@ConfigurationProperties(prefix = "openfoodfacts")
public record OpenFoodFactsProperties(boolean enabled, String baseUrl, Double minConfidence) {

    public String baseUrlOrDefault() {
        return (baseUrl == null || baseUrl.isBlank()) ? "https://world.openfoodfacts.org" : baseUrl;
    }

    public double minConfidenceOrDefault() {
        return minConfidence == null ? 0.6 : minConfidence;
    }
}
