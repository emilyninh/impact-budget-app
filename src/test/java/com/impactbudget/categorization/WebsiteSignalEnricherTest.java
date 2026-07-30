package com.impactbudget.categorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebsiteSignalEnricherTest {

    private WebsiteSignalEnricher enricher() {
        return new WebsiteSignalEnricher(
                new WebSignalProperties(true, 4000, 5),
                new ScoringProperties("none"),          // no Ollama rationale call
                new OllamaProperties(null, null),
                new ObjectMapper(),
                new SimpleMeterRegistry());
    }

    private MerchantScoring neutral() {
        return new MerchantScoring("Simpleecology", "Shopping", 40, false, 50,
                List.of(), 0.2, "neutral", MerchantScoring.SOURCE_FALLBACK);
    }

    @Test
    void detectsCertificationsAndTheirFlags() {
        String page = "<html>we use gots certified organic cotton and recycled packaging, "
                + "a certified b corporation, oeko-tex tested.</html>";
        WebsiteSignalEnricher.Scan scan = WebsiteSignalEnricher.scanSignals(page);
        assertThat(scan.flags()).contains("gots-certified", "organic", "recycled", "b-corp", "oeko-tex");
        assertThat(scan.weight()).isGreaterThanOrEqualTo(45);
    }

    @Test
    void findsNothingInAnUnrelatedPage() {
        WebsiteSignalEnricher.Scan scan = WebsiteSignalEnricher.scanSignals(
                "<html>fast fashion trends, lowest prices, free shipping</html>");
        assertThat(scan.weight()).isZero();
        assertThat(scan.flags()).isEmpty();
    }

    @Test
    void skipsWikidataDemotedMultinationalsWithoutFetching() {
        MerchantScoring demoted = new MerchantScoring("Shein", "Shopping", 8, false, 20,
                List.of(), 0.85, "multinational", MerchantScoring.SOURCE_WIKIDATA);
        // localScore < 30 guard → returns the base unchanged, no network.
        assertThat(enricher().enrich("shein.com", "SHEIN.COM", "Shein", demoted)).isSameAs(demoted);
    }

    @Test
    void returnsBaseWhenNoDomainResolves() {
        MerchantScoring base = neutral();
        // Offline merchant, no domain anywhere → no fetch, base unchanged.
        assertThat(enricher().enrich(null, "STARBUCKS STORE 452", "Starbucks", base)).isSameAs(base);
    }
}
