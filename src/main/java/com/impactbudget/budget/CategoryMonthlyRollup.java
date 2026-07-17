package com.impactbudget.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Per-(user, month, category) spend rollup, maintained incrementally by the budget consumer. */
@Entity
@Table(name = "category_monthly_rollup")
@IdClass(CategoryRollupId.class)
public class CategoryMonthlyRollup {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "year_month")
    private String yearMonth;

    @Id
    @Column(name = "category")
    private String category;

    @Column(name = "total_spend", nullable = false)
    private BigDecimal totalSpend;

    @Column(name = "txn_count", nullable = false)
    private int txnCount;

    @Column(name = "sustainability_weighted", nullable = false)
    private BigDecimal sustainabilityWeighted;

    public String getUserId() {
        return userId;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getTotalSpend() {
        return totalSpend;
    }

    public int getTxnCount() {
        return txnCount;
    }

    public BigDecimal getSustainabilityWeighted() {
        return sustainabilityWeighted;
    }
}
