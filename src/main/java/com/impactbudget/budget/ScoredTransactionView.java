package com.impactbudget.budget;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A row for the dashboard transaction list. */
public record ScoredTransactionView(
        String merchantName,
        String category,
        LocalDate txnDate,
        BigDecimal amount,
        int localScore,
        int sustainabilityScore,
        boolean localIndependent
) {
    static ScoredTransactionView from(ScoredTransaction st) {
        return new ScoredTransactionView(
                st.getMerchantName(), st.getCategory(), st.getTxnDate(), st.getAmount(),
                st.getLocalScore(), st.getSustainabilityScore(), st.isLocalIndependent());
    }
}
