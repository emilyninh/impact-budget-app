package com.impactbudget.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A linked Plaid Item (one financial institution connection for a user). Holds the
 * access token and the transactions-sync cursor so re-syncs resume where they left off.
 */
@Entity
@Table(name = "plaid_item")
public class PlaidItem {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "plaid_item_id", nullable = false, unique = true)
    private String plaidItemId;

    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Column(name = "institution_name")
    private String institutionName;

    @Column(name = "transactions_cursor", columnDefinition = "text")
    private String transactionsCursor;

    /** While set (and in the future), the backfill job keeps re-syncing this newly-linked item. */
    @Column(name = "backfill_until")
    private Instant backfillUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPlaidItemId() {
        return plaidItemId;
    }

    public void setPlaidItemId(String plaidItemId) {
        this.plaidItemId = plaidItemId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getTransactionsCursor() {
        return transactionsCursor;
    }

    public void setTransactionsCursor(String transactionsCursor) {
        this.transactionsCursor = transactionsCursor;
    }

    public Instant getBackfillUntil() {
        return backfillUntil;
    }

    public void setBackfillUntil(Instant backfillUntil) {
        this.backfillUntil = backfillUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
