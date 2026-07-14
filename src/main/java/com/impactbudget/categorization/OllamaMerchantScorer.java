package com.impactbudget.categorization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Scores merchants with a local <a href="https://ollama.com">Ollama</a> model — free, private
 * (nothing leaves the machine), and no API key. Selected by {@code categorization.scoring.provider=ollama}
 * (the default). Uses Ollama's {@code /api/chat} with {@code format: "json"} to force parseable
 * output. Returns the neutral fallback if the server is unreachable or the reply is malformed.
 */
@Component
class OllamaMerchantScorer implements MerchantScorer {

    private static final Logger log = LoggerFactory.getLogger(OllamaMerchantScorer.class);

    private final OllamaProperties props;
    private final LlmScoringSupport support;
    private final ObjectMapper mapper;
    private final RestClient http;

    OllamaMerchantScorer(OllamaProperties props, LlmScoringSupport support, ObjectMapper mapper) {
        this.props = props;
        this.support = support;
        this.mapper = mapper;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(30));   // generation can take a few seconds
        this.http = RestClient.builder().baseUrl(props.baseUrlOrDefault()).requestFactory(factory).build();
    }

    @Override
    public String providerName() {
        return "ollama";
    }

    @Override
    public MerchantScoring score(String normalized, String rawMerchant) {
        try {
            Map<String, Object> body = Map.of(
                    "model", props.modelOrDefault(),
                    "stream", false,
                    "format", "json",
                    "messages", List.of(
                            Map.of("role", "system", "content", support.systemPrompt()),
                            Map.of("role", "user", "content", support.userPrompt(normalized, rawMerchant))));

            String response = http.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = mapper.readTree(response);
            String content = root.path("message").path("content").asText("");
            return support.parse(content, rawMerchant);
        } catch (Exception e) {
            log.warn("Ollama scoring failed for '{}' ({}); using fallback. Is `ollama serve` running with model '{}'?",
                    rawMerchant, e.toString(), props.modelOrDefault());
            return support.neutral(rawMerchant);
        }
    }
}
