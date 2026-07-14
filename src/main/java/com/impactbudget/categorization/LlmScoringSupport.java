package com.impactbudget.categorization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Shared prompt and JSON parsing for LLM-based merchant scorers (Claude, Ollama, …). Keeps
 * the scoring contract identical across providers so they're interchangeable.
 */
@Component
public class LlmScoringSupport {

    static final String SYSTEM_PROMPT = """
            You are a merchant classifier for a values-based budgeting app. Given a bank
            merchant descriptor, identify the real merchant and assess two dimensions.

            Respond with ONLY a JSON object (no prose, no markdown fences) matching exactly:
            {
              "cleanedMerchant": string,
              "category": string,
              "localScore": integer 0-100,          // 0 = multinational conglomerate, 100 = local independently-owned
              "localIndependent": boolean,
              "sustainabilityScore": integer 0-100,  // 0 = high-footprint / fast fashion / synthetic, 100 = B-Corp / organic / natural fiber / plant-based
              "materialFlags": [string],             // e.g. ["polyester","organic","merino-wool"]
              "confidence": number 0-1,
              "rationale": string                    // one short sentence
            }
            If unsure, use moderate scores and low confidence.
            """;

    private final ObjectMapper mapper;

    public LlmScoringSupport(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String userPrompt(String normalized, String rawMerchant) {
        return "Merchant descriptor: \"" + rawMerchant + "\"\n"
                + "Normalized: \"" + normalized + "\"\n"
                + "Return the JSON now.";
    }

    /** Parse an LLM's JSON reply into a scoring (source = LLM). Throws on malformed output. */
    public MerchantScoring parse(String text, String rawMerchant) throws Exception {
        String json = text;
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        LlmJson j = mapper.readValue(json, LlmJson.class);

        int local = clamp(j.localScore() != null ? j.localScore() : 40);
        int sustainability = clamp(j.sustainabilityScore() != null ? j.sustainabilityScore() : 50);
        boolean independent = Boolean.TRUE.equals(j.localIndependent());
        List<String> flags = j.materialFlags() != null ? j.materialFlags() : List.of();
        double confidence = j.confidence() != null ? j.confidence() : 0.5;
        String cleaned = StringUtils.hasText(j.cleanedMerchant()) ? j.cleanedMerchant() : rawMerchant;

        return new MerchantScoring(cleaned, j.category(), local, independent, sustainability,
                flags, confidence, j.rationale(), MerchantScoring.SOURCE_LLM);
    }

    /** Neutral heuristic used when no scorer is available or a call fails. */
    public MerchantScoring neutral(String rawMerchant) {
        return new MerchantScoring(rawMerchant, null, 40, false, 50, List.of(), 0.2,
                "Neutral heuristic (no scorer available).", MerchantScoring.SOURCE_FALLBACK);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    /** Shape of the JSON an LLM returns; extra/missing fields tolerated. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record LlmJson(
            String cleanedMerchant,
            String category,
            Integer localScore,
            Boolean localIndependent,
            Integer sustainabilityScore,
            List<String> materialFlags,
            Double confidence,
            String rationale) {
    }
}
