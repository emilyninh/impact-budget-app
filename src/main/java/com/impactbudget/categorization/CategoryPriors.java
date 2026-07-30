package com.impactbudget.categorization;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Replaces the flat neutral fallback (local 40 / sustainability 50) with a directional baseline
 * derived from the transaction's Plaid category, so an unknown airline doesn't score the same as an
 * unknown independent service. Applied only when nothing grounded (or the LLM) had an opinion — i.e.
 * the base scoring is still {@link MerchantScoring#SOURCE_FALLBACK}. The result stays low-confidence
 * (0.35): a smarter guess, not a measurement, so it nudges the point estimate without being counted
 * as "scored" coverage. Grounded/LLM scores pass through unchanged.
 */
@Component
public class CategoryPriors {

    /** Confidence for a category-prior estimate — above a flat guess (0.2), below the LLM (0.5). */
    static final double PRIOR_CONFIDENCE = 0.35;

    private record Prior(int local, int sustainability) {
    }

    private static final Prior DEFAULT = new Prior(40, 50);

    // Rough, directional priors keyed on Plaid PFC primary. Values are deliberately modest; grounded
    // enrichers (website certs, Open Food Facts, curated) override on top with real confidence.
    private static final Map<String, Prior> PRIORS = Map.ofEntries(
            Map.entry("TRAVEL", new Prior(30, 30)),                      // flights/hotels — high footprint
            Map.entry("TRANSPORTATION", new Prior(30, 35)),             // gas / rideshare
            Map.entry("GENERAL_MERCHANDISE", new Prior(35, 45)),        // big-box / online retail leans chain
            Map.entry("GENERAL_SERVICES", new Prior(55, 50)),          // services skew local/independent
            Map.entry("PERSONAL_CARE", new Prior(50, 50)),
            Map.entry("MEDICAL", new Prior(45, 50)),
            Map.entry("FOOD_AND_DRINK", new Prior(50, 50)),            // many independent cafes/restaurants
            Map.entry("ENTERTAINMENT", new Prior(45, 50)),
            Map.entry("HOME_IMPROVEMENT", new Prior(40, 45)),
            Map.entry("RENT_AND_UTILITIES", new Prior(40, 45)),
            Map.entry("LOAN_PAYMENTS", new Prior(30, 50)),
            Map.entry("BANK_FEES", new Prior(30, 50)),
            Map.entry("GOVERNMENT_AND_NON_PROFIT", new Prior(50, 60))); // non-profits skew positive

    /**
     * @param base       the base scoring (from the LLM or the neutral fallback)
     * @param pfcPrimary Plaid PFC primary category (may be null for CSV/demo)
     */
    public MerchantScoring apply(MerchantScoring base, String pfcPrimary) {
        if (!MerchantScoring.SOURCE_FALLBACK.equals(base.source())) {
            return base;   // an LLM or grounded source already spoke — don't overwrite it
        }
        Prior p = pfcPrimary == null ? DEFAULT
                : PRIORS.getOrDefault(pfcPrimary.toUpperCase(Locale.ROOT).trim(), DEFAULT);
        String rationale = "Category baseline for "
                + (pfcPrimary == null ? "unknown category" : pfcPrimary.toLowerCase(Locale.ROOT));
        return new MerchantScoring(base.cleanedMerchant(), base.category(),
                p.local(), base.localIndependent(), p.sustainability(), base.materialFlags(),
                PRIOR_CONFIDENCE, rationale, MerchantScoring.SOURCE_FALLBACK);
    }
}
