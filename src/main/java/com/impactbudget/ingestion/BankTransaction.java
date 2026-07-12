package com.impactbudget.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Getter
@Setter
@NoArgsConstructor
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

    @Column(name = "location_city")
    private String locationCity;

    @Column(name = "location_region")
    private String locationRegion;

    @Column(name = "pending", nullable = false)
    private boolean pending;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
