package com.impactbudget.categorization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantScoreRepository extends JpaRepository<MerchantScore, UUID> {

    Optional<MerchantScore> findByNormalizedMerchant(String normalizedMerchant);

    /** Highest-sustainability known merchants in a category — candidates for "greener swaps". */
    List<MerchantScore> findTop10ByCategoryAndSustainabilityScoreGreaterThanEqualOrderBySustainabilityScoreDesc(
            String category, int minSustainabilityScore);
}
