package com.impactbudget.categorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WikidataLocalEnricherTest {

    @Test
    void looseMatchIgnoresCaseSpacingAndPunctuation() {
        assertThat(WikidataLocalEnricher.looseMatch("STARBUCKS", "Starbucks")).isTrue();
        assertThat(WikidataLocalEnricher.looseMatch("STARBUCKS", "Starbucks Corporation")).isTrue();
        assertThat(WikidataLocalEnricher.looseMatch("BEN JERRY", "Ben & Jerry's")).isTrue();
        assertThat(WikidataLocalEnricher.looseMatch("LOCAL COFFEE", "Starbucks")).isFalse();
        assertThat(WikidataLocalEnricher.looseMatch("AB", "Ab")).isFalse();   // too short
    }

    @Test
    void descriptionKeywordsFlagChains() {
        assertThat(WikidataLocalEnricher.descriptionMatchesChain(
                "American multinational chain of coffeehouses")).isTrue();
        assertThat(WikidataLocalEnricher.descriptionMatchesChain(
                "multinational retail corporation")).isTrue();
        assertThat(WikidataLocalEnricher.descriptionMatchesChain(
                "independent coffee shop in Portland")).isFalse();
        assertThat(WikidataLocalEnricher.descriptionMatchesChain(null)).isFalse();
    }

    @Test
    void whenDisabledItReturnsTheBaseScoringUnchanged() {
        var enricher = new WikidataLocalEnricher(
                new WikidataProperties(false, null),   // disabled → no HTTP client, no network
                new ObjectMapper(), new SimpleMeterRegistry());
        MerchantScoring base = new MerchantScoring("Starbucks", "Coffee", 70, true, 45,
                List.of(), 0.5, "guess", MerchantScoring.SOURCE_LLM);

        assertThat(enricher.enrich("STARBUCKS", base)).isSameAs(base);
    }
}
