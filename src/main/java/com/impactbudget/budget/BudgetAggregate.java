package com.impactbudget.budget;

import java.math.BigDecimal;

/**
 * A user's monthly impact aggregate.
 *
 * @param localImpactPct          confidence-weighted local score, 0–100 (weights grounded scores
 *                                over guesses so unknowns don't drag it to the middle)
 * @param sustainabilityImpactPct confidence-weighted sustainability score, 0–100
 * @param localIndependentSpend   dollars spent at merchants flagged local &amp; independent
 * @param scoredSharePct          share of spend that is actually scored (confidence above the floor),
 *                                0–100 — the honesty caption for the two percentages above
 */
public record BudgetAggregate(
        String userId,
        String yearMonth,
        BigDecimal totalSpend,
        double localImpactPct,
        double sustainabilityImpactPct,
        BigDecimal localIndependentSpend,
        long transactionCount,
        double scoredSharePct
) {
}
