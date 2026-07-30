package com.impactbudget.categorization;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryPriorsTest {

    private final CategoryPriors priors = new CategoryPriors();

    private MerchantScoring fallback() {
        return new MerchantScoring("Some Merchant", "Shopping", 40, false, 50,
                List.of(), 0.2, "neutral", MerchantScoring.SOURCE_FALLBACK);
    }

    @Test
    void appliesADirectionalBaselineToFallbackScores() {
        MerchantScoring travel = priors.apply(fallback(), "TRAVEL");
        assertThat(travel.sustainabilityScore()).isEqualTo(30);   // flights are high-footprint
        assertThat(travel.confidence()).isEqualTo(CategoryPriors.PRIOR_CONFIDENCE);
        assertThat(travel.source()).isEqualTo(MerchantScoring.SOURCE_FALLBACK);

        MerchantScoring services = priors.apply(fallback(), "GENERAL_SERVICES");
        assertThat(services.localScore()).isEqualTo(55);          // services skew local/independent
    }

    @Test
    void unknownCategoryFallsBackToTheDefaultBaseline() {
        MerchantScoring d = priors.apply(fallback(), null);
        assertThat(d.localScore()).isEqualTo(40);
        assertThat(d.sustainabilityScore()).isEqualTo(50);
        assertThat(d.confidence()).isEqualTo(CategoryPriors.PRIOR_CONFIDENCE);
    }

    @Test
    void leavesGroundedOrLlmScoresUntouched() {
        MerchantScoring llm = new MerchantScoring("Merchant", "Shopping", 85, true, 60,
                List.of(), 0.5, "llm opinion", MerchantScoring.SOURCE_LLM);
        // A real opinion already exists — the prior must not overwrite it.
        assertThat(priors.apply(llm, "TRAVEL")).isSameAs(llm);
    }
}
