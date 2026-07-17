package com.impactbudget.dashboard;

import com.impactbudget.budget.BudgetAggregateService;
import com.impactbudget.budget.ScoredTransactionView;
import com.impactbudget.categorization.GreenerAlternative;
import com.impactbudget.categorization.SwapSuggestionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds "greener swap" suggestions: for a user's lower-sustainability purchases, propose
 * higher-scoring merchants in the same category. Reads the user's recent transactions from the
 * budget module and the alternatives from the categorization module.
 */
@Service
class RecommendationService {

    /** Only transactions scoring below this are considered worth improving. */
    private static final int IMPROVABLE_BELOW = 55;
    /** An alternative must beat the current score by at least this margin. */
    private static final int MIN_MARGIN = 15;
    private static final int MAX_SWAPS = 6;
    private static final int SUGGESTIONS_PER_SWAP = 3;

    private final BudgetAggregateService aggregateService;
    private final SwapSuggestionService swapSuggestionService;

    RecommendationService(BudgetAggregateService aggregateService,
                          SwapSuggestionService swapSuggestionService) {
        this.aggregateService = aggregateService;
        this.swapSuggestionService = swapSuggestionService;
    }

    List<Swap> greenerSwaps(String userId, String yearMonth) {
        List<Swap> swaps = new ArrayList<>();
        Set<String> seenMerchants = new HashSet<>();

        for (ScoredTransactionView txn : aggregateService.recentTransactions(userId, yearMonth)) {
            if (swaps.size() >= MAX_SWAPS) {
                break;
            }
            if (txn.sustainabilityScore() >= IMPROVABLE_BELOW
                    || txn.category() == null || txn.merchantName() == null) {
                continue;
            }
            String key = txn.merchantName().toLowerCase(Locale.ROOT);
            if (!seenMerchants.add(key)) {
                continue;   // one suggestion per merchant
            }
            List<GreenerAlternative> alternatives = swapSuggestionService.greenerAlternatives(
                    txn.category(), txn.sustainabilityScore() + MIN_MARGIN,
                    txn.merchantName(), SUGGESTIONS_PER_SWAP);
            if (!alternatives.isEmpty()) {
                swaps.add(new Swap(txn.merchantName(), txn.category(),
                        txn.sustainabilityScore(), alternatives));
            }
        }
        return swaps;
    }

    /** A low-scoring purchase and greener alternatives for it. */
    record Swap(String fromMerchant, String category, int fromScore, List<GreenerAlternative> suggestions) {
    }
}
