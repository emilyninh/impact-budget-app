package com.impactbudget.categorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenFoodFactsEnricherTest {

    @Test
    void ecoScoreGradesMapToNumbers() {
        assertThat(OpenFoodFactsEnricher.gradeToScore("a")).isEqualTo(90);
        assertThat(OpenFoodFactsEnricher.gradeToScore("C")).isEqualTo(55);
        assertThat(OpenFoodFactsEnricher.gradeToScore("e")).isEqualTo(15);
        assertThat(OpenFoodFactsEnricher.gradeToScore("unknown")).isNull();
        assertThat(OpenFoodFactsEnricher.gradeToScore(null)).isNull();
    }

    @Test
    void knownLabelsMapToMaterialFlags() throws Exception {
        var product = new ObjectMapper().readTree("""
                {"labels_tags":["en:organic","en:fair-trade","en:some-other-label"]}
                """);

        List<String> flags = OpenFoodFactsEnricher.labelFlags(product);

        assertThat(flags).containsExactlyInAnyOrder("organic", "fair-trade");
    }

    @Test
    void whenDisabledItReturnsTheBaseScoringUnchanged() {
        var enricher = new OpenFoodFactsEnricher(
                new OpenFoodFactsProperties(false, null, null),   // disabled → no HTTP client, no network
                new ObjectMapper(), new SimpleMeterRegistry());
        MerchantScoring base = new MerchantScoring("Rosie's Cafe", "Coffee", 90, true, 55,
                List.of(), 0.5, "local", MerchantScoring.SOURCE_LLM);

        assertThat(enricher.enrich("Rosie's Cafe", base)).isSameAs(base);
    }
}
