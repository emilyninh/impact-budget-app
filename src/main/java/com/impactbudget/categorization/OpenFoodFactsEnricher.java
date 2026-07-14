package com.impactbudget.categorization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Overlays a real sustainability score from <a href="https://world.openfoodfacts.org">Open Food
 * Facts</a> — free and key-less — when a merchant/brand matches food or packaged-goods products.
 * Only the sustainability dimension and material flags are affected; the local score from the
 * base scorer is preserved. On no match or any error the base scoring is returned unchanged.
 *
 * <p>Limitation: bank transactions name a <em>merchant</em>, not a product, so this helps most
 * for grocery/CPG brands and is a no-op for e.g. restaurants. Curated overrides still win on top.
 */
@Component
public class OpenFoodFactsEnricher {

    private static final Logger log = LoggerFactory.getLogger(OpenFoodFactsEnricher.class);

    private final OpenFoodFactsProperties props;
    private final ObjectMapper mapper;
    private final MeterRegistry meterRegistry;
    private final RestClient http;   // null when disabled

    public OpenFoodFactsEnricher(OpenFoodFactsProperties props, ObjectMapper mapper, MeterRegistry meterRegistry) {
        this.props = props;
        this.mapper = mapper;
        this.meterRegistry = meterRegistry;
        if (props.enabled()) {
            var factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(2));
            factory.setReadTimeout(Duration.ofSeconds(5));
            this.http = RestClient.builder().baseUrl(props.baseUrlOrDefault())
                    .requestFactory(factory)
                    .defaultHeader("User-Agent", "ImpactBudget/0.1 (portfolio project)")
                    .build();
        } else {
            this.http = null;
        }
    }

    public MerchantScoring enrich(String query, MerchantScoring base) {
        if (http == null || query == null || query.isBlank()) {
            return base;
        }
        try {
            String response = http.get()
                    .uri(uri -> uri.path("/cgi/search.pl")
                            .queryParam("search_terms", query)
                            .queryParam("search_simple", "1")
                            .queryParam("action", "process")
                            .queryParam("json", "1")
                            .queryParam("page_size", "10")
                            .queryParam("fields",
                                    "product_name,brands,ecoscore_score,ecoscore_grade,"
                                            + "environmental_score_score,environmental_score_grade,labels_tags")
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode products = mapper.readTree(response).path("products");
            if (!products.isArray() || products.isEmpty()) {
                return miss(base);
            }

            List<Integer> scores = new ArrayList<>();
            Set<String> flags = new LinkedHashSet<>(base.materialFlags());
            for (JsonNode product : products) {
                Integer eco = ecoScore(product);
                if (eco != null) {
                    scores.add(eco);
                }
                flags.addAll(labelFlags(product));
            }
            if (scores.isEmpty()) {
                return miss(base);
            }

            int avg = (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(base.sustainabilityScore()));
            meterRegistry.counter("categorization.openfoodfacts", "result", "hit").increment();

            String source = MerchantScoring.SOURCE_FALLBACK.equals(base.source())
                    ? MerchantScoring.SOURCE_OPENFOODFACTS : base.source();
            String rationale = (base.rationale() != null ? base.rationale() + " | " : "")
                    + "Open Food Facts eco-score from " + scores.size() + " product(s)";

            return new MerchantScoring(
                    base.cleanedMerchant(), base.category(), base.localScore(), base.localIndependent(),
                    avg, List.copyOf(flags), Math.max(base.confidence(), props.minConfidenceOrDefault()),
                    rationale, source);
        } catch (Exception e) {
            log.debug("Open Food Facts enrichment failed for '{}' ({})", query, e.toString());
            return base;
        }
    }

    private MerchantScoring miss(MerchantScoring base) {
        meterRegistry.counter("categorization.openfoodfacts", "result", "miss").increment();
        return base;
    }

    /** Prefer the numeric eco/environmental score (0–100); fall back to the letter grade. */
    private Integer ecoScore(JsonNode product) {
        if (product.hasNonNull("ecoscore_score")) {
            return clamp(product.get("ecoscore_score").asInt());
        }
        if (product.hasNonNull("environmental_score_score")) {
            return clamp(product.get("environmental_score_score").asInt());
        }
        String grade = product.path("ecoscore_grade").asText(
                product.path("environmental_score_grade").asText(""));
        return gradeToScore(grade);
    }

    static Integer gradeToScore(String grade) {
        return switch (grade == null ? "" : grade.toLowerCase()) {
            case "a", "a-plus" -> 90;
            case "b" -> 75;
            case "c" -> 55;
            case "d" -> 35;
            case "e" -> 15;
            default -> null;   // "unknown", "not-applicable", etc.
        };
    }

    static List<String> labelFlags(JsonNode product) {
        List<String> out = new ArrayList<>();
        for (JsonNode tag : product.path("labels_tags")) {
            switch (tag.asText()) {
                case "en:organic", "en:eu-organic", "en:usda-organic" -> out.add("organic");
                case "en:fair-trade", "en:fairtrade-international" -> out.add("fair-trade");
                case "en:vegan" -> out.add("vegan");
                case "en:carbon-neutral" -> out.add("carbon-neutral");
                default -> { /* ignore other labels */ }
            }
        }
        return out;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
