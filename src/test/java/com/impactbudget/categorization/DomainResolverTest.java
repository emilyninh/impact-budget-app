package com.impactbudget.categorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainResolverTest {

    @Test
    void prefersPlaidWebsiteWithoutVerification() {
        DomainResolver.Candidate c = DomainResolver.resolve("https://www.SimpleEcology.com/pages/about",
                "WWW.SIMPLEECOLOGY.COM", "Simpleecology");
        assertThat(c.domain()).isEqualTo("simpleecology.com");
        assertThat(c.needsVerification()).isFalse();
    }

    @Test
    void extractsDomainFromDescriptor() {
        DomainResolver.Candidate c = DomainResolver.resolve(null, "WWW.SIMPLEECOLOGY.COM", "Simpleecology");
        assertThat(c.domain()).isEqualTo("simpleecology.com");
        assertThat(c.needsVerification()).isFalse();

        assertThat(DomainResolver.resolve(null, "SP WWW.TRYSURI.COM", "Suri").domain())
                .isEqualTo("trysuri.com");
    }

    @Test
    void guessesFromNameForShopifyMerchantsAndFlagsForVerification() {
        DomainResolver.Candidate c = DomainResolver.resolve(null, "SP LINA LENNOX", "Lina Lennox");
        assertThat(c.domain()).isEqualTo("linalennox.com");
        assertThat(c.needsVerification()).isTrue();
    }

    @Test
    void doesNotGuessForOfflineMerchants() {
        // No domain, not a Shopify "SP" descriptor → don't fabricate a website.
        assertThat(DomainResolver.resolve(null, "STARBUCKS STORE 452", "Starbucks")).isNull();
    }

    @Test
    void normalizeStripsSchemeWwwAndPath() {
        assertThat(DomainResolver.normalize("HTTPS://WWW.Foo-Bar.com/why-us")).isEqualTo("foo-bar.com");
    }
}
