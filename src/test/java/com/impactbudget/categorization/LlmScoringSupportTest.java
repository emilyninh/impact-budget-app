package com.impactbudget.categorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmScoringSupportTest {

    private final LlmScoringSupport support = new LlmScoringSupport(new ObjectMapper());

    @Test
    void parsesPlainJsonAndClampsScores() throws Exception {
        String json = """
                {"cleanedMerchant":"Blue Bottle","category":"Coffee","localScore":150,
                 "localIndependent":true,"sustainabilityScore":-5,
                 "materialFlags":["organic"],"confidence":0.8,"rationale":"indie roaster"}
                """;

        MerchantScoring s = support.parse(json, "SQ*BLUE BOTTLE");

        assertThat(s.cleanedMerchant()).isEqualTo("Blue Bottle");
        assertThat(s.localScore()).isEqualTo(100);          // clamped from 150
        assertThat(s.sustainabilityScore()).isEqualTo(0);   // clamped from -5
        assertThat(s.localIndependent()).isTrue();
        assertThat(s.source()).isEqualTo(MerchantScoring.SOURCE_LLM);
    }

    @Test
    void toleratesMarkdownFencesAndSurroundingProse() throws Exception {
        String reply = """
                Here you go:
                ```json
                {"cleanedMerchant":"Patagonia","sustainabilityScore":95,"localIndependent":false}
                ```
                """;

        MerchantScoring s = support.parse(reply, "PATAGONIA");

        assertThat(s.cleanedMerchant()).isEqualTo("Patagonia");
        assertThat(s.sustainabilityScore()).isEqualTo(95);
        assertThat(s.localScore()).isEqualTo(40);   // default when absent
    }

    @Test
    void neutralIsAModerateLowConfidenceFallback() {
        MerchantScoring s = support.neutral("UNKNOWN MERCHANT");

        assertThat(s.source()).isEqualTo(MerchantScoring.SOURCE_FALLBACK);
        assertThat(s.localScore()).isEqualTo(40);
        assertThat(s.sustainabilityScore()).isEqualTo(50);
        assertThat(s.confidence()).isLessThan(0.5);
    }
}
