package com.impactbudget.dashboard;

import com.impactbudget.budget.BudgetAggregateService;
import com.impactbudget.budget.ScoredTransactionView;
import com.impactbudget.categorization.GreenerAlternative;
import com.impactbudget.categorization.SwapSuggestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    BudgetAggregateService aggregateService;
    @Mock
    SwapSuggestionService swapSuggestionService;

    private ScoredTransactionView txn(String merchant, String category, int sustainability) {
        return new ScoredTransactionView(merchant, category, LocalDate.of(2026, 7, 1),
                new BigDecimal("40.00"), 20, sustainability, false, "Chase", false);
    }

    @Test
    void suggestsSwapsOnlyForLowScoringMerchants() {
        when(aggregateService.recentTransactions("u", "2026-07")).thenReturn(List.of(
                txn("Shein", "Shopping", 15),        // low — improvable
                txn("Patagonia", "Shopping", 95)));  // already high — skipped
        when(swapSuggestionService.greenerAlternatives(eq("Shopping"), anyInt(), eq("Shein"), anyInt()))
                .thenReturn(List.of(new GreenerAlternative("Patagonia", 95, List.of("organic"))));

        List<RecommendationService.Swap> swaps =
                new RecommendationService(aggregateService, swapSuggestionService).greenerSwaps("u", "2026-07");

        assertThat(swaps).hasSize(1);
        assertThat(swaps.get(0).fromMerchant()).isEqualTo("Shein");
        assertThat(swaps.get(0).fromScore()).isEqualTo(15);
        assertThat(swaps.get(0).suggestions()).extracting(GreenerAlternative::merchant).containsExactly("Patagonia");
    }

    @Test
    void skipsLowScorerWhenNoAlternativesExist() {
        when(aggregateService.recentTransactions("u", "2026-07")).thenReturn(List.of(
                txn("Mystery Diner", "Eating Out", 20)));
        when(swapSuggestionService.greenerAlternatives(eq("Eating Out"), anyInt(), eq("Mystery Diner"), anyInt()))
                .thenReturn(List.of());

        List<RecommendationService.Swap> swaps =
                new RecommendationService(aggregateService, swapSuggestionService).greenerSwaps("u", "2026-07");

        assertThat(swaps).isEmpty();
    }
}
