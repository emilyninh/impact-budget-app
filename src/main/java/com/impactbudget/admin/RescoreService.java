package com.impactbudget.admin;

import com.impactbudget.budget.BudgetAggregateService;
import com.impactbudget.budget.ScoredTransaction;
import com.impactbudget.budget.ScoredTransactionRepository;
import com.impactbudget.categorization.CategorizationService;
import com.impactbudget.categorization.MerchantNormalizer;
import com.impactbudget.categorization.MerchantScoring;
import com.impactbudget.ingestion.BankTransaction;
import com.impactbudget.ingestion.BankTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Re-scores a user's already-loaded transactions so scoring improvements (notably the website-signal
 * enricher) reach data that was scored before the change. Merchant scores are cached per merchant, so
 * new logic otherwise only affects future merchants; this refreshes the cache and rewrites the
 * per-transaction impact scores, then invalidates the user's budget aggregates.
 *
 * <p>Each distinct merchant is re-scored once (that's where the web fetch happens); the per-row DB
 * writes auto-commit individually, so the network work never holds a long-lived transaction open.
 */
@Service
public class RescoreService {

    private static final Logger log = LoggerFactory.getLogger(RescoreService.class);

    private final ScoredTransactionRepository scoredRepository;
    private final BankTransactionRepository bankRepository;
    private final com.impactbudget.categorization.ImpactScoreRepository impactScoreRepository;
    private final CategorizationService categorizationService;
    private final BudgetAggregateService aggregateService;

    public RescoreService(ScoredTransactionRepository scoredRepository,
                          BankTransactionRepository bankRepository,
                          com.impactbudget.categorization.ImpactScoreRepository impactScoreRepository,
                          CategorizationService categorizationService,
                          BudgetAggregateService aggregateService) {
        this.scoredRepository = scoredRepository;
        this.bankRepository = bankRepository;
        this.impactScoreRepository = impactScoreRepository;
        this.categorizationService = categorizationService;
        this.aggregateService = aggregateService;
    }

    /** Re-score every transaction for a user. Returns the number of distinct merchants re-scored. */
    public int rescore(String userId) {
        List<ScoredTransaction> rows = scoredRepository.findByUserId(userId);
        Map<String, MerchantScoring> byMerchant = new HashMap<>();

        for (ScoredTransaction st : rows) {
            BankTransaction bt = bankRepository.findById(st.getTransactionId()).orElse(null);
            if (bt == null) {
                continue;
            }
            // Re-score this merchant once (does the web fetch); reuse for its other transactions.
            String key = MerchantNormalizer.normalize(bt.getMerchantRaw());
            MerchantScoring s = byMerchant.computeIfAbsent(key, k -> categorizationService.rescoreMerchant(
                    bt.getMerchantRaw(), bt.getMerchantWebsite(), bt.getMerchantName()));

            st.setLocalScore(s.localScore());
            st.setSustainabilityScore(s.sustainabilityScore());
            st.setLocalIndependent(s.localIndependent());
            scoredRepository.save(st);

            impactScoreRepository.findByTransactionId(st.getTransactionId()).ifPresent(is -> {
                is.setLocalScore(s.localScore());
                is.setSustainabilityScore(s.sustainabilityScore());
                is.setLocalIndependent(s.localIndependent());
                is.setMaterialFlags(s.materialFlags().isEmpty() ? null : String.join(",", s.materialFlags()));
                impactScoreRepository.save(is);
            });
        }

        aggregateService.invalidateUser(userId);
        log.info("Re-scored {} transactions across {} merchants for {}",
                rows.size(), byMerchant.size(), userId);
        return byMerchant.size();
    }
}
