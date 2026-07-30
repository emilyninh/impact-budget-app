package com.impactbudget.categorization;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config for the free web-signal enricher, which fetches a small online merchant's own website and
 * detects sustainability certifications (GOTS, organic, B Corp, …) to raise an otherwise-neutral
 * score. Key-less; disable with {@code web-signal.enabled=false} if outbound fetches aren't wanted.
 */
@ConfigurationProperties(prefix = "web-signal")
public record WebSignalProperties(Boolean enabled, Integer timeoutMs, Integer maxPages) {

    public boolean enabledOrDefault() {
        return enabled == null || enabled;   // default on
    }

    public int timeoutMsOrDefault() {
        return timeoutMs == null ? 4000 : timeoutMs;
    }

    public int maxPagesOrDefault() {
        return maxPages == null ? 5 : maxPages;
    }
}
