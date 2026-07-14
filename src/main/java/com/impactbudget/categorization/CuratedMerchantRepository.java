package com.impactbudget.categorization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CuratedMerchantRepository extends JpaRepository<CuratedMerchant, UUID> {

    List<CuratedMerchant> findAll();

    boolean existsByMatchKey(String matchKey);
}
