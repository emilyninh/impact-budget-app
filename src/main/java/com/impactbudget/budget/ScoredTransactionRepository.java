package com.impactbudget.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScoredTransactionRepository extends JpaRepository<ScoredTransaction, UUID> {

    boolean existsByTransactionId(UUID transactionId);

    List<ScoredTransaction> findByUserIdAndYearMonth(String userId, String yearMonth);
}
