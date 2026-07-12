package com.impactbudget.budget;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A row for the dashboard transaction list. */
public record ScoredTransactionView(
        String merchantName,
        LocalDate txnDate,
        BigDecimal amount,
        int localScore,
        int sustainabilityScore,
        boolean localIndependent
) {
    static ScoredTransactionView from(ScoredTransaction st) {
        return new ScoredTransactionView(
                st.getMerchantName(), st.getTxnDate(), st.getAmount(),
                st.getLocalScore(), st.getSustainabilityScore(), st.isLocalIndependent());
    }
}
