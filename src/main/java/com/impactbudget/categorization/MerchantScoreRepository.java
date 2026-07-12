package com.impactbudget.categorization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantScoreRepository extends JpaRepository<MerchantScore, UUID> {

    Optional<MerchantScore> findByNormalizedMerchant(String normalizedMerchant);
}
