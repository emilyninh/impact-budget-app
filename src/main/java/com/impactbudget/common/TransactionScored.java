package com.impactbudget.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Event published after a transaction has been scored for impact. Consumed by the budget
 * module to update a user's monthly local/sustainable spending aggregates.
 *
 * <p>Carries the amount and date so the budget module can aggregate without a DB lookup.
 */
public record TransactionScored(
        UUID transactionId,
        String userId,
        BigDecimal amount,
        LocalDate txnDate,
        String category,
        int localScore,
        boolean localIndependent,
        int sustainabilityScore,
        List<String> materialFlags,
        double confidence,
        String source
) {
}
