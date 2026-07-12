package com.impactbudget.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {

    Optional<BankTransaction> findByPlaidTransactionId(String plaidTransactionId);

    void deleteByPlaidTransactionId(String plaidTransactionId);
}
