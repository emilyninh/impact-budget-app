package com.impactbudget.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Event published when a new bank transaction has been persisted. Consumed by the
 * categorization engine (to score it) and the budget module (to update aggregates) —
 * the "one event, two consumers" fan-out.
 *
 * <p>Lives in {@code common} as part of the shared event schema so both producer and
 * consumers depend on a single definition.
 */
public record TransactionIngested(
        UUID transactionId,
        String userId,
        String merchantRaw,
        String merchantName,
        BigDecimal amount,
        String isoCurrency,
        LocalDate txnDate,
        String locationCity,
        String locationRegion,
        String sourceCategory,          // bank-provided category (Plaid PFC primary or a CSV column), a scoring/taxonomy hint; may be null
        String sourceCategoryDetailed,  // Plaid PFC detailed (e.g. FOOD_AND_DRINK_GROCERIES); null for CSV/demo
        String institutionName          // source bank (e.g. "Chase", "Capital One"); may be null
) {
}
