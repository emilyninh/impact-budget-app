package com.impactbudget.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpendBudgetRepository extends JpaRepository<SpendBudget, UUID> {

    Optional<SpendBudget> findByUserId(String userId);
}
