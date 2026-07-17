package com.impactbudget.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface CategoryMonthlyRollupRepository
        extends JpaRepository<CategoryMonthlyRollup, CategoryRollupId> {

    /** Atomic increment of a category's rollup (INSERT or accumulate on conflict). */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO category_monthly_rollup
                (user_id, year_month, category, total_spend, txn_count, sustainability_weighted)
            VALUES (:userId, :yearMonth, :category, :amount, 1, :sustainabilityWeighted)
            ON CONFLICT (user_id, year_month, category) DO UPDATE SET
                total_spend = category_monthly_rollup.total_spend + EXCLUDED.total_spend,
                txn_count = category_monthly_rollup.txn_count + 1,
                sustainability_weighted = category_monthly_rollup.sustainability_weighted
                    + EXCLUDED.sustainability_weighted
            """, nativeQuery = true)
    void accumulate(@Param("userId") String userId,
                    @Param("yearMonth") String yearMonth,
                    @Param("category") String category,
                    @Param("amount") BigDecimal amount,
                    @Param("sustainabilityWeighted") BigDecimal sustainabilityWeighted);

    List<CategoryMonthlyRollup> findByUserIdAndYearMonthOrderByTotalSpendDesc(String userId, String yearMonth);
}
