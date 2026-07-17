package com.impactbudget.budget;

import java.math.BigDecimal;

/**
 * Snapshot of a user's spending against their monthly budget for a given month.
 *
 * @param monthlyLimit   the set limit, or {@code null} when no budget is configured
 * @param spent          total spend so far this month
 * @param remaining      {@code monthlyLimit - spent} (may be negative), or {@code null} if unset
 * @param pctUsed        spent as a percentage of the limit (0 if unset)
 * @param daysElapsed    days counted so far (full month for a past month)
 * @param daysInMonth    calendar days in the month
 * @param projectedSpend end-of-month spend projected from the current daily pace
 * @param status         classification for the UI to color the bar
 */
public record BudgetStatus(
        String userId,
        String yearMonth,
        BigDecimal monthlyLimit,
        BigDecimal spent,
        BigDecimal remaining,
        double pctUsed,
        int daysElapsed,
        int daysInMonth,
        BigDecimal projectedSpend,
        Status status
) {
    public enum Status {
        /** No monthly limit set yet. */
        NO_BUDGET,
        /** Actual spend already exceeds the limit. */
        OVER,
        /** Within the limit today, but the current pace projects past it by month-end. */
        AT_RISK,
        /** On pace to finish within the limit. */
        ON_TRACK
    }
}
