package com.impactbudget.budget;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A goal plus its live progress. {@code progressPct} is how far the user has moved from
 * their baseline toward the target (0–100), based on the current month's aggregate.
 */
public record GoalProgress(
        UUID goalId,
        Goal.Dimension dimension,
        int baselinePct,
        int targetPct,
        double currentPct,
        LocalDate targetDate,
        double progressPct,
        boolean achieved
) {
}
