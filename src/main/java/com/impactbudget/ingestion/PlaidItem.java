package com.impactbudget.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Getter
@Setter
@NoArgsConstructor
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
