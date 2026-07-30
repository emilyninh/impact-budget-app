package com.impactbudget.admin;

import com.impactbudget.budget.BudgetAggregateService;
import com.impactbudget.budget.CategoryMonthlyRollupRepository;
import com.impactbudget.budget.ScoredTransaction;
import com.impactbudget.budget.ScoredTransactionRepository;
import com.impactbudget.categorization.MerchantCategoryResolver;
import com.impactbudget.categorization.PlaidPfcMapper;
import com.impactbudget.ingestion.BankTransaction;
import com.impactbudget.ingestion.BankTransactionRepository;
import com.impactbudget.ingestion.PlaidGateway;
import com.impactbudget.ingestion.PlaidItem;
import com.impactbudget.ingestion.PlaidItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * Re-derives the category, transfer-exclusion flag, and source institution for a user's already
 * scored transactions, then rebuilds their category rollups and drops the cached aggregates.
 *
 * <p>Needed because the categorization improvements (Plaid-PFC taxonomy + transfer exclusion) only
 * affect transactions ingested <em>after</em> the change — a Plaid re-sync won't re-emit rows it
 * has already synced. This recomputes in place from data already stored on {@code bank_transaction}
 * (the Plaid PFC + the linked item's institution), <em>without</em> re-running the LLM: impact
 * scores are untouched, only the category dimension changes.
 *
 * <p>Note: historical rows predate {@code plaid_category_detailed}, so the Groceries vs Eating Out
 * split falls back to merchant keywords for old data; the user's named cases (flights = TRAVEL,
 * rent = RENT_AND_UTILITIES, transfers = TRANSFER_OUT) resolve from the stored PFC primary alone.
 */
@Service
public class RecategorizeService {

    private static final Logger log = LoggerFactory.getLogger(RecategorizeService.class);

    private final ScoredTransactionRepository scoredRepository;
    private final BankTransactionRepository bankRepository;
    private final CategoryMonthlyRollupRepository rollupRepository;
    private final PlaidItemRepository itemRepository;
    private final PlaidGateway plaidGateway;
    private final PlaidPfcMapper pfcMapper;
    private final MerchantCategoryResolver categoryResolver;
    private final BudgetAggregateService aggregateService;

    public RecategorizeService(ScoredTransactionRepository scoredRepository,
                               BankTransactionRepository bankRepository,
                               CategoryMonthlyRollupRepository rollupRepository,
                               PlaidItemRepository itemRepository,
                               PlaidGateway plaidGateway,
                               PlaidPfcMapper pfcMapper,
                               MerchantCategoryResolver categoryResolver,
                               BudgetAggregateService aggregateService) {
        this.scoredRepository = scoredRepository;
        this.bankRepository = bankRepository;
        this.rollupRepository = rollupRepository;
        this.itemRepository = itemRepository;
        this.plaidGateway = plaidGateway;
        this.pfcMapper = pfcMapper;
        this.categoryResolver = categoryResolver;
        this.aggregateService = aggregateService;
    }

    /** Recompute categories for a single user's transactions. Returns the number of rows updated. */
    @Transactional
    public int recategorize(String userId) {
        backfillInstitutions(userId);
        List<ScoredTransaction> rows = scoredRepository.findByUserId(userId);
        rollupRepository.deleteByUserId(userId);   // rebuilt below from the fresh categories

        int updated = 0;
        for (ScoredTransaction st : rows) {
            BankTransaction bt = bankRepository.findById(st.getTransactionId()).orElse(null);
            String primary = bt != null ? bt.getPlaidCategory() : null;
            String detailed = bt != null ? bt.getPlaidCategoryDetailed() : null;
            String institution = (bt != null && bt.getPlaidItem() != null)
                    ? bt.getPlaidItem().getInstitutionName()
                    : st.getInstitutionName();

            String category = pfcMapper.map(primary, detailed);
            if (category == null && PlaidPfcMapper.isFoodAndDrink(primary)) {
                // Historical FOOD_AND_DRINK lacking detailed — split groceries vs eating out by name.
                category = categoryResolver.resolveFoodByMerchant(st.getMerchantName());
            }
            if (category == null) {
                category = categoryResolver.resolve(st.getMerchantName(), primary);
            }
            boolean excluded = MerchantCategoryResolver.TRANSFERS.equals(category);

            st.setCategory(category);
            st.setExcludedFromSpend(excluded);
            st.setInstitutionName(institution);
            scoredRepository.save(st);
            updated++;

            if (!excluded) {
                rollupRepository.accumulate(userId, st.getYearMonth(), category, st.getAmount(),
                        st.getAmount().multiply(BigDecimal.valueOf(st.getSustainabilityScore())));
            }
        }

        aggregateService.invalidateUser(userId);
        log.info("Re-categorized {} transactions for {}", updated, userId);
        return updated;
    }

    /**
     * Fill in the institution name (e.g. "Chase") for the user's real Plaid items that were linked
     * before we captured it. Best-effort per item — a Plaid failure just leaves that item unlabeled.
     */
    private void backfillInstitutions(String userId) {
        for (PlaidItem item : itemRepository.findByUserId(userId)) {
            String token = item.getAccessToken();
            if (token == null || !token.startsWith("access-")) {
                continue;   // synthetic CSV/demo item — its institution is already set
            }
            if (StringUtils.hasText(item.getInstitutionName())) {
                continue;   // already known
            }
            String name = plaidGateway.fetchInstitutionName(token);
            if (StringUtils.hasText(name)) {
                item.setInstitutionName(name);
                itemRepository.save(item);
                log.info("Backfilled institution '{}' for item {}", name, item.getPlaidItemId());
            }
        }
    }
}
