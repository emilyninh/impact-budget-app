package com.impactbudget.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaidItemRepository extends JpaRepository<PlaidItem, UUID> {

    Optional<PlaidItem> findByPlaidItemId(String plaidItemId);

    List<PlaidItem> findByUserId(String userId);

    /** Items still in their post-link backfill window (see PlaidBackfillJob). */
    List<PlaidItem> findByBackfillUntilIsNotNull();
}
