package com.impactbudget.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /** Oldest unpublished events first, capped so each relay tick does bounded work. */
    List<OutboxEvent> findTop200ByPublishedAtIsNullOrderByCreatedAtAsc();

    long countByPublishedAtIsNull();
}
