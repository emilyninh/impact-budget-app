package com.impactbudget.categorization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImpactScoreRepository extends JpaRepository<ImpactScore, UUID> {

    Optional<ImpactScore> findByTransactionId(UUID transactionId);
}
