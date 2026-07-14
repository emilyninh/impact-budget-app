package com.impactbudget.categorization;

import java.util.List;

/**
 * Resolved impact assessment for a merchant, from any source (cache, LLM, or curated
 * override). Scores are 0–100.
 *
 * @param localScore          0 = multinational conglomerate, 100 = local/independent
 * @param sustainabilityScore 0 = high-footprint / fast fashion, 100 = B-Corp / organic / natural
 * @param source              LLM | FALLBACK | CURATED | CACHE
 */
public record MerchantScoring(
        String cleanedMerchant,
        String category,
        int localScore,
        boolean localIndependent,
        int sustainabilityScore,
        List<String> materialFlags,
        double confidence,
        String rationale,
        String source
) {
    public static final String SOURCE_LLM = "LLM";
    public static final String SOURCE_FALLBACK = "FALLBACK";
    public static final String SOURCE_CURATED = "CURATED";
    public static final String SOURCE_CACHE = "CACHE";
    public static final String SOURCE_OPENFOODFACTS = "OFF";
    public static final String SOURCE_WIKIDATA = "WIKI";
}
