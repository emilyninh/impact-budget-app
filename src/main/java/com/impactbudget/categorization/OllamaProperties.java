package com.impactbudget.categorization;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config for a local <a href="https://ollama.com">Ollama</a> server — a free, private,
 * no-API-key LLM used as the categorization fallback. Run e.g. {@code ollama run llama3.1}.
 */
@ConfigurationProperties(prefix = "ollama")
public record OllamaProperties(String baseUrl, String model) {

    public String baseUrlOrDefault() {
        return (baseUrl == null || baseUrl.isBlank()) ? "http://localhost:11434" : baseUrl;
    }

    public String modelOrDefault() {
        return (model == null || model.isBlank()) ? "llama3.1" : model;
    }
}
