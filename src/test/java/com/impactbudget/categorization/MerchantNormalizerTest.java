package com.impactbudget.categorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantNormalizerTest {

    @Test
    void stripsProcessorPrefixesAndStoreIds() {
        assertThat(MerchantNormalizer.normalize("TST*SQ*LOCAL COFFEE 12345")).isEqualTo("LOCAL COFFEE");
        assertThat(MerchantNormalizer.normalize("SQ *BLUE BOTTLE #0042")).isEqualTo("BLUE BOTTLE");
        assertThat(MerchantNormalizer.normalize("AMAZON.COM*A1B2C3")).isEqualTo("AMAZON");
    }

    @Test
    void handlesBlankAndNull() {
        assertThat(MerchantNormalizer.normalize(null)).isEmpty();
        assertThat(MerchantNormalizer.normalize("   ")).isEmpty();
    }

    @Test
    void isStableAcrossCasingAndPunctuation() {
        assertThat(MerchantNormalizer.normalize("patagonia, inc."))
                .isEqualTo(MerchantNormalizer.normalize("PATAGONIA INC"));
    }
}
