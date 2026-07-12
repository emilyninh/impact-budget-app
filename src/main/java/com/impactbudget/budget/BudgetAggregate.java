package com.impactbudget.budget;

import java.math.BigDecimal;

/**
 * A user's monthly impact aggregate.
 *
 * @param localImpactPct          spend-weighted local score, 0–100 (how "local" the month's spending was)
 * @param sustainabilityImpactPct spend-weighted sustainability score, 0–100
 * @param localIndependentSpend   dollars spent at merchants flagged local &amp; independent
 */
public record BudgetAggregate(
        String userId,
        String yearMonth,
        BigDecimal totalSpend,
        double localImpactPct,
        double sustainabilityImpactPct,
        BigDecimal localIndependentSpend,
        long transactionCount
) {
}
