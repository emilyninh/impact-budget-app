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
import java.util.Set;

/**
 * Overlays the <em>local</em> dimension using free, key-less <a href="https://www.wikidata.org">
 * Wikidata</a> data. If a merchant matches a known chain — evidenced by a parent organization
 * (property {@code P749}) or a description that reads like a chain/multinational/retailer —
 * it's demoted to a low local score. This is what OpenStreetMap's {@code brand:wikidata} tags
 * ultimately point at, so we query Wikidata directly.
 *
 * <p>Conservative by design: it only <em>demotes</em> confident chain matches. A merchant with
 * no Wikidata match (typical of a genuinely local business) is left unchanged. Curated overrides
 * still win on top.
 */
@Component
public class WikidataLocalEnricher {

    private static final Logger log = LoggerFactory.getLogger(WikidataLocalEnricher.class);

    private static final Set<String> CHAIN_KEYWORDS = Set.of(
            "chain", "multinational", "corporation", "conglomerate", "retailer",
            "franchise", "fast food", "fast-food", "big-box");

    private final WikidataProperties props;
    private final ObjectMapper mapper;
    private final MeterRegistry meterRegistry;
    private final RestClient http;   // null when disabled

    public WikidataLocalEnricher(WikidataProperties props, ObjectMapper mapper, MeterRegistry meterRegistry) {
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
            JsonNode hit = bestSearchHit(query);
            if (hit == null) {
                return miss(base);
            }
            String id = hit.path("id").asText();
            String label = hit.path("label").asText(query);
            String description = hit.path("description").asText("");

            boolean hasParent = hasParentOrganization(id);
            boolean chainByDescription = descriptionMatchesChain(description);
            if (!hasParent && !chainByDescription) {
                return miss(base);
            }

            meterRegistry.counter("categorization.wikidata", "result", "hit").increment();
            int localScore = hasParent ? 8 : 15;   // parent org = clear chain; keyword-only = likely chain
            double confidence = Math.max(base.confidence(), hasParent ? 0.85 : 0.7);
            String source = MerchantScoring.SOURCE_FALLBACK.equals(base.source())
                    ? MerchantScoring.SOURCE_WIKIDATA : base.source();
            String rationale = (base.rationale() != null ? base.rationale() + " | " : "")
                    + "Wikidata: " + label + (hasParent ? " — has a parent organization" : " — matches a chain profile");

            return new MerchantScoring(
                    base.cleanedMerchant(), base.category(), localScore, false,
                    base.sustainabilityScore(), base.materialFlags(), confidence, rationale, source);
        } catch (Exception e) {
            log.debug("Wikidata enrichment failed for '{}' ({})", query, e.toString());
            return base;
        }
    }

    /** Top search hit whose label loosely matches the query, else null. */
    private JsonNode bestSearchHit(String query) {
        String response = http.get()
                .uri(uri -> uri.path("/w/api.php")
                        .queryParam("action", "wbsearchentities")
                        .queryParam("search", query)
                        .queryParam("language", "en")
                        .queryParam("uselang", "en")
                        .queryParam("type", "item")
                        .queryParam("limit", "5")
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .body(String.class);
        try {
            JsonNode results = mapper.readTree(response).path("search");
            for (JsonNode hit : results) {
                if (looseMatch(query, hit.path("label").asText(""))) {
                    return hit;
                }
            }
        } catch (Exception e) {
            log.debug("Wikidata search parse failed ({})", e.toString());
        }
        return null;
    }

    /** True if the entity has a parent organization (P749) claim. */
    private boolean hasParentOrganization(String entityId) {
        try {
            String response = http.get()
                    .uri(uri -> uri.path("/w/api.php")
                            .queryParam("action", "wbgetclaims")
                            .queryParam("entity", entityId)
                            .queryParam("property", "P749")
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(String.class);
            JsonNode p749 = mapper.readTree(response).path("claims").path("P749");
            return p749.isArray() && !p749.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private MerchantScoring miss(MerchantScoring base) {
        meterRegistry.counter("categorization.wikidata", "result", "miss").increment();
        return base;
    }

    /** Case/space/punctuation-insensitive containment either way (min length 3 to avoid noise). */
    static boolean looseMatch(String query, String label) {
        String a = squash(query);
        String b = squash(label);
        if (a.length() < 3 || b.length() < 3) {
            return false;
        }
        return a.contains(b) || b.contains(a);
    }

    static boolean descriptionMatchesChain(String description) {
        if (description == null || description.isBlank()) {
            return false;
        }
        String lower = description.toLowerCase();
        return CHAIN_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private static String squash(String s) {
        return s == null ? "" : s.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
