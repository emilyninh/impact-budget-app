package com.impactbudget.categorization;

/**
 * A pluggable merchant scorer. Implementations must always return a result — on any failure
 * (no API key, server down, bad response) they return a neutral fallback rather than throwing,
 * so the categorization pipeline never blocks.
 */
public interface MerchantScorer {

    /** Selector name matched against {@code categorization.scoring.provider}. */
    String providerName();

    MerchantScoring score(String normalizedMerchant, String rawMerchant);
}
