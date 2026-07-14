package com.impactbudget.categorization;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config for the free, key-less <a href="https://www.wikidata.org">Wikidata</a> lookup used
 * to demote known chains / multinational-owned brands on the <em>local</em> dimension.
 */
@ConfigurationProperties(prefix = "wikidata")
public record WikidataProperties(boolean enabled, String baseUrl) {

    public String baseUrlOrDefault() {
        return (baseUrl == null || baseUrl.isBlank()) ? "https://www.wikidata.org" : baseUrl;
    }
}
