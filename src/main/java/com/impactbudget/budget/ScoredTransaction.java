package com.impactbudget.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Budget-owned projection of a scored transaction, populated from {@code TransactionScored}
 * events. Monthly aggregates are rebuilt from this table, so the budget module never has to
 * query the ingestion or categorization tables. Uniquely keyed by transaction for idempotency.
 */
@Entity
@Table(name = "scored_transaction")
public class ScoredTransaction {

    @Id
    private UUID id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private UUID transactionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "year_month", nullable = false)
    private String yearMonth;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "local_score", nullable = false)
    private int localScore;

    @Column(name = "sustainability_score", nullable = false)
    private int sustainabilityScore;

    @Column(name = "local_independent", nullable = false)
    private boolean localIndependent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getLocalScore() {
        return localScore;
    }

    public void setLocalScore(int localScore) {
        this.localScore = localScore;
    }

    public int getSustainabilityScore() {
        return sustainabilityScore;
    }

    public void setSustainabilityScore(int sustainabilityScore) {
        this.sustainabilityScore = sustainabilityScore;
    }

    public boolean isLocalIndependent() {
        return localIndependent;
    }

    public void setLocalIndependent(boolean localIndependent) {
        this.localIndependent = localIndependent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
