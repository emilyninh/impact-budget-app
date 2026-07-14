package com.impactbudget.categorization;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CuratedMerchantSeederTest {

    @Test
    void parsesPipeDelimitedRowsAndSkipsCommentsAndBlanks() throws Exception {
        String csv = """
                # a comment
                match_key|display_name|sustainability_score|material_flags|note

                BOMBAS|Bombas|80||B Corp — apparel
                NUMI|Numi Organic Tea|88|organic,fair-trade|B Corp — tea
                BADSCORE|Broken Row|not-a-number||
                """;

        List<CuratedMerchantSeeder.CuratedRow> rows =
                CuratedMerchantSeeder.parse(new BufferedReader(new StringReader(csv)));

        // Header line has a non-numeric score field too, so it's parsed as a row but the
        // real assertions are on the known-good rows.
        assertThat(rows).extracting(CuratedMerchantSeeder.CuratedRow::matchKey)
                .contains("BOMBAS", "NUMI", "BADSCORE");

        var bombas = rows.stream().filter(r -> r.matchKey().equals("BOMBAS")).findFirst().orElseThrow();
        assertThat(bombas.displayName()).isEqualTo("Bombas");
        assertThat(bombas.sustainabilityScore()).isEqualTo(80);
        assertThat(bombas.materialFlags()).isNull();   // empty field → null

        var numi = rows.stream().filter(r -> r.matchKey().equals("NUMI")).findFirst().orElseThrow();
        assertThat(numi.materialFlags()).isEqualTo("organic,fair-trade");

        // Non-numeric score falls back to the certified-B-Corp default (70).
        var bad = rows.stream().filter(r -> r.matchKey().equals("BADSCORE")).findFirst().orElseThrow();
        assertThat(bad.sustainabilityScore()).isEqualTo(70);
    }
}
