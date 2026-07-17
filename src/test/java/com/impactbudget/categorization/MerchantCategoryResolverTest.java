package com.impactbudget.categorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantCategoryResolverTest {

    private final MerchantCategoryResolver resolver = new MerchantCategoryResolver();

    @Test
    void mapsFreeTextLlmCategoryOntoTaxonomy() {
        assertThat(resolver.resolve("Starbucks", "Coffee")).isEqualTo(MerchantCategoryResolver.EATING_OUT);
        assertThat(resolver.resolve("X", "Eating Out")).isEqualTo(MerchantCategoryResolver.EATING_OUT);
    }

    @Test
    void fallsBackToMerchantNameWhenLlmCategoryIsNullOrBlank() {
        assertThat(resolver.resolve("Whole Foods Market", null)).isEqualTo(MerchantCategoryResolver.GROCERIES);
        assertThat(resolver.resolve("Patagonia", "")).isEqualTo(MerchantCategoryResolver.SHOPPING);
        assertThat(resolver.resolve("Netflix", null)).isEqualTo(MerchantCategoryResolver.SUBSCRIPTIONS);
        assertThat(resolver.resolve("Uber", null)).isEqualTo(MerchantCategoryResolver.TRANSPORT);
    }

    @Test
    void llmCategoryTakesPrecedenceOverMerchantName() {
        // Amazon would keyword-match Shopping, but the LLM said Groceries — trust the LLM.
        assertThat(resolver.resolve("Amazon", "Groceries")).isEqualTo(MerchantCategoryResolver.GROCERIES);
    }

    @Test
    void unknownMerchantWithNoLlmCategoryIsOther() {
        assertThat(resolver.resolve("Zzz Mystery LLC", null)).isEqualTo(MerchantCategoryResolver.OTHER);
        assertThat(resolver.resolve(null, null)).isEqualTo(MerchantCategoryResolver.OTHER);
    }
}
