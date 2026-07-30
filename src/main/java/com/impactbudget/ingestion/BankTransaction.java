package com.impactbudget.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A raw bank transaction synced from Plaid. Named {@code BankTransaction} to avoid
 * clashing with Plaid's own {@code com.plaid.client.model.Transaction} model.
 *
 * <p>{@code plaidTransactionId} is uniquely constrained so redelivered webhooks or
 * re-syncs never duplicate a row (idempotency at the database level).
 */
@Entity
@Table(name = "bank_transaction")
public class BankTransaction {

    @Id
    private UUID id;

    @Column(name = "plaid_transaction_id", nullable = false, unique = true)
    private String plaidTransactionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plaid_item_id", nullable = false)
    private PlaidItem plaidItem;

    @Column(name = "user_id", nullable = false)
    private String userId;

    /** Raw descriptor from the bank, e.g. {@code TST*SQ*LOCAL COFFEE 12345}. */
    @Column(name = "merchant_raw", nullable = false)
    private String merchantRaw;

    /** Plaid's cleaned merchant name, when available. */
    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "iso_currency")
    private String isoCurrency;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(name = "plaid_category")
    private String plaidCategory;

    /** Plaid's finer {@code personal_finance_category.detailed}, e.g. FOOD_AND_DRINK_GROCERIES. */
    @Column(name = "plaid_category_detailed")
    private String plaidCategoryDetailed;

    @Column(name = "location_city")
    private String locationCity;

    @Column(name = "location_region")
    private String locationRegion;

    @Column(name = "pending", nullable = false)
    private boolean pending;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPlaidTransactionId() {
        return plaidTransactionId;
    }

    public void setPlaidTransactionId(String plaidTransactionId) {
        this.plaidTransactionId = plaidTransactionId;
    }

    public PlaidItem getPlaidItem() {
        return plaidItem;
    }

    public void setPlaidItem(PlaidItem plaidItem) {
        this.plaidItem = plaidItem;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMerchantRaw() {
        return merchantRaw;
    }

    public void setMerchantRaw(String merchantRaw) {
        this.merchantRaw = merchantRaw;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getIsoCurrency() {
        return isoCurrency;
    }

    public void setIsoCurrency(String isoCurrency) {
        this.isoCurrency = isoCurrency;
    }

    public LocalDate getTxnDate() {
        return txnDate;
    }

    public void setTxnDate(LocalDate txnDate) {
        this.txnDate = txnDate;
    }

    public String getPlaidCategory() {
        return plaidCategory;
    }

    public void setPlaidCategory(String plaidCategory) {
        this.plaidCategory = plaidCategory;
    }

    public String getPlaidCategoryDetailed() {
        return plaidCategoryDetailed;
    }

    public void setPlaidCategoryDetailed(String plaidCategoryDetailed) {
        this.plaidCategoryDetailed = plaidCategoryDetailed;
    }

    public String getLocationCity() {
        return locationCity;
    }

    public void setLocationCity(String locationCity) {
        this.locationCity = locationCity;
    }

    public String getLocationRegion() {
        return locationRegion;
    }

    public void setLocationRegion(String locationRegion) {
        this.locationRegion = locationRegion;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
