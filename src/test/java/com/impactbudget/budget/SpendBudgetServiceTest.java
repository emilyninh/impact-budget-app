package com.impactbudget.budget;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SpendBudgetServiceTest {

    // evaluate() uses no repositories/clock, so nulls are fine here.
    private final SpendBudgetService service = new SpendBudgetService(null, null);

    @Test
    void projectsPastPaceAndFlagsAtRiskMidMonth() {
        // $300 spent by day 15 of a 30-day month → projected $600 > $500 limit.
        BudgetStatus s = service.evaluate("u", "2026-07",
                new BigDecimal("300.00"), new BigDecimal("500.00"), 15, 30);

        assertThat(s.status()).isEqualTo(BudgetStatus.Status.AT_RISK);
        assertThat(s.projectedSpend()).isEqualByComparingTo("600.00");
        assertThat(s.remaining()).isEqualByComparingTo("200.00");
        assertThat(s.pctUsed()).isEqualTo(60.0);
    }

    @Test
    void onTrackWhenProjectionStaysUnderLimit() {
        // $200 by day 15 → projected $400 < $500.
        BudgetStatus s = service.evaluate("u", "2026-07",
                new BigDecimal("200.00"), new BigDecimal("500.00"), 15, 30);

        assertThat(s.status()).isEqualTo(BudgetStatus.Status.ON_TRACK);
        assertThat(s.projectedSpend()).isEqualByComparingTo("400.00");
    }

    @Test
    void overWhenActualSpendExceedsLimit() {
        BudgetStatus s = service.evaluate("u", "2026-07",
                new BigDecimal("620.00"), new BigDecimal("500.00"), 15, 30);

        assertThat(s.status()).isEqualTo(BudgetStatus.Status.OVER);
        assertThat(s.remaining()).isEqualByComparingTo("-120.00");
    }

    @Test
    void completeMonthProjectsActualNotExtrapolated() {
        // Full month elapsed → projection is the actual total, no extrapolation.
        BudgetStatus s = service.evaluate("u", "2026-06",
                new BigDecimal("400.00"), new BigDecimal("500.00"), 30, 30);

        assertThat(s.projectedSpend()).isEqualByComparingTo("400.00");
        assertThat(s.status()).isEqualTo(BudgetStatus.Status.ON_TRACK);
    }

    @Test
    void doesNotExtrapolateInTheFirstWeek() {
        // Day 1: a naive daily-pace projection would report ~$27k (871.45 × 31); before a week has
        // elapsed the rate is noise, so report the actual spend instead — and no bogus AT_RISK.
        BudgetStatus s = service.evaluate("u", "2026-08",
                new BigDecimal("871.45"), new BigDecimal("1000.00"), 1, 31);

        assertThat(s.projectedSpend()).isEqualByComparingTo("871.45");
        assertThat(s.status()).isEqualTo(BudgetStatus.Status.ON_TRACK);
    }

    @Test
    void noLimitYieldsNoBudgetStatus() {
        BudgetStatus s = service.evaluate("u", "2026-07",
                new BigDecimal("123.45"), null, 15, 30);

        assertThat(s.status()).isEqualTo(BudgetStatus.Status.NO_BUDGET);
        assertThat(s.monthlyLimit()).isNull();
        assertThat(s.remaining()).isNull();
        assertThat(s.pctUsed()).isEqualTo(0.0);
        assertThat(s.spent()).isEqualByComparingTo("123.45");
    }
}
