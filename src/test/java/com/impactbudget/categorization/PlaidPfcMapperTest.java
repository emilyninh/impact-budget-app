package com.impactbudget.categorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaidPfcMapperTest {

    private final PlaidPfcMapper mapper = new PlaidPfcMapper();

    @Test
    void mapsTheUsersNamedCasesFromPrimaryAlone() {
        assertThat(mapper.map("TRAVEL", null)).isEqualTo(MerchantCategoryResolver.TRAVEL);
        assertThat(mapper.map("RENT_AND_UTILITIES", "RENT_AND_UTILITIES_RENT"))
                .isEqualTo(MerchantCategoryResolver.HOUSING);
        assertThat(mapper.map("TRANSFER_OUT", null)).isEqualTo(MerchantCategoryResolver.TRANSFERS);
        assertThat(mapper.map("TRANSFER_IN", null)).isEqualTo(MerchantCategoryResolver.TRANSFERS);
    }

    @Test
    void splitsFoodByDetailed() {
        assertThat(mapper.map("FOOD_AND_DRINK", "FOOD_AND_DRINK_GROCERIES"))
                .isEqualTo(MerchantCategoryResolver.GROCERIES);
        assertThat(mapper.map("FOOD_AND_DRINK", "FOOD_AND_DRINK_RESTAURANT"))
                .isEqualTo(MerchantCategoryResolver.EATING_OUT);
        // Detailed missing (historical rows): defer (null) so the caller can split by merchant name.
        assertThat(mapper.map("FOOD_AND_DRINK", null)).isNull();
        assertThat(PlaidPfcMapper.isFoodAndDrink("FOOD_AND_DRINK")).isTrue();
        assertThat(PlaidPfcMapper.isFoodAndDrink("TRANSPORTATION")).isFalse();
    }

    @Test
    void splitsUtilitiesAndLoansByDetailed() {
        assertThat(mapper.map("RENT_AND_UTILITIES", "RENT_AND_UTILITIES_GAS_AND_ELECTRICITY"))
                .isEqualTo(MerchantCategoryResolver.BILLS);
        assertThat(mapper.map("LOAN_PAYMENTS", "LOAN_PAYMENTS_MORTGAGE_PAYMENT"))
                .isEqualTo(MerchantCategoryResolver.HOUSING);
        assertThat(mapper.map("LOAN_PAYMENTS", "LOAN_PAYMENTS_CAR_PAYMENT"))
                .isEqualTo(MerchantCategoryResolver.BILLS);
    }

    @Test
    void streamingReadsAsSubscription() {
        assertThat(mapper.map("ENTERTAINMENT", "ENTERTAINMENT_TV_AND_MOVIES"))
                .isEqualTo(MerchantCategoryResolver.SUBSCRIPTIONS);
        assertThat(mapper.map("ENTERTAINMENT", "ENTERTAINMENT_SPORTING_EVENTS"))
                .isEqualTo(MerchantCategoryResolver.ENTERTAINMENT);
    }

    @Test
    void returnsNullWhenUnmappedOrAbsentSoCallerCanFallBack() {
        assertThat(mapper.map(null, null)).isNull();
        assertThat(mapper.map("", null)).isNull();
        assertThat(mapper.map("GENERAL_SERVICES", "GENERAL_SERVICES_CONSULTING")).isNull();
    }
}
