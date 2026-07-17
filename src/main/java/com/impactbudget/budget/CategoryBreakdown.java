package com.impactbudget.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A category's spend for a month, with the spend-weighted average sustainability score. */
public record CategoryBreakdown(
        String category,
        BigDecimal totalSpend,
        int txnCount,
        double avgSustainability
) {
    static CategoryBreakdown from(CategoryMonthlyRollup r) {
        double avg = r.getTotalSpend().signum() > 0
                ? r.getSustainabilityWeighted()
                    .divide(r.getTotalSpend(), 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        return new CategoryBreakdown(r.getCategory(),
                r.getTotalSpend().setScale(2, RoundingMode.HALF_UP), r.getTxnCount(), avg);
    }
}
