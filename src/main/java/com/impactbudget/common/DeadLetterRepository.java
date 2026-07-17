package com.impactbudget.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeadLetterRepository extends JpaRepository<DeadLetter, UUID> {

    List<DeadLetter> findByReplayedAtIsNullOrderByCreatedAtAsc();

    long countByReplayedAtIsNull();
}
