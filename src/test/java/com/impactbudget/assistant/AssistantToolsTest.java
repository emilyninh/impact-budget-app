package com.impactbudget.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.impactbudget.budget.BudgetAggregateService;
import com.impactbudget.budget.BudgetStatus;
import com.impactbudget.budget.CategoryBreakdown;
import com.impactbudget.categorization.CategorizationService;
import com.impactbudget.categorization.MerchantScoreRepository;
import com.impactbudget.categorization.MerchantScoring;
import com.impactbudget.categorization.SwapSuggestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the grounded tool layer — verifies each tool returns the right real numbers as
 * JSON, without any LLM call. The Anthropic loop itself is not exercised here.
 */
class AssistantToolsTest {

    private static final String USER = "11111111-1111-1111-1111-111111111111";

    private final ObjectMapper mapper = new ObjectMapper();
    private BudgetAggregateService aggregates;
    private CategorizationService categorization;
    private MerchantScoreRepository merchantScores;
    private SwapSuggestionService swaps;

    private BudgetStatus stubStatus;
    private AssistantTools toolsUnderTest;

    @BeforeEach
    void setUp() {
        aggregates = mock(BudgetAggregateService.class);
        categorization = mock(CategorizationService.class);
        merchantScores = mock(MerchantScoreRepository.class);
        swaps = mock(SwapSuggestionService.class);
        // Inject the budget status via the test seam so we don't need the whole SpendBudgetService.
        toolsUnderTest = new AssistantTools(
                (uid, ym) -> stubStatus, aggregates, categorization, merchantScores, swaps, mapper);
    }

    @Test
    void budgetStatusReportsOverBudgetAndAmountOver() throws Exception {
        stubStatus = new BudgetStatus(USER, "2026-08",
                new BigDecimal("500.00"), new BigDecimal("606.95"), new BigDecimal("-106.95"),
                121.4, 1, 31, new BigDecimal("18815.45"), BudgetStatus.Status.OVER);

        JsonNode out = mapper.readTree(toolsUnderTest.execute(USER, "get_budget_status", Map.of()));

        assertThat(out.get("overBudget").asBoolean()).isTrue();
        assertThat(out.get("amountOver").asDouble()).isEqualTo(106.95);
        assertThat(out.get("spent").asDouble()).isEqualTo(606.95);
        assertThat(out.get("status").asText()).isEqualTo("OVER");
    }

    @Test
    void categorySpendFiltersToTheRequestedCategory() throws Exception {
        when(aggregates.categoryBreakdown(eq(USER), anyString())).thenReturn(List.of(
                new CategoryBreakdown("Shopping", new BigDecimal("312.00"), 5, 88.0),
                new CategoryBreakdown("Groceries", new BigDecimal("130.70"), 3, 79.0)));

        JsonNode out = mapper.readTree(
                toolsUnderTest.execute(USER, "get_category_spend", Map.of("category", "groceries")));

        JsonNode categories = out.get("categories");
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).get("category").asText()).isEqualTo("Groceries");
        assertThat(categories.get(0).get("totalSpend").asDouble()).isEqualTo(130.70);
    }

    @Test
    void scoreStoreScoresLiveWhenNotInTransactions() throws Exception {
        when(merchantScores.findByNormalizedMerchant(anyString())).thenReturn(Optional.empty());
        when(categorization.rescoreMerchant(eq("Allbirds"), any(), any(), eq("Allbirds")))
                .thenReturn(new MerchantScoring("Allbirds", "Shopping", 8, false, 88,
                        List.of("b-corp", "natural-fiber"), 0.99, "Certified B Corp",
                        MerchantScoring.SOURCE_CURATED));

        JsonNode out = mapper.readTree(
                toolsUnderTest.execute(USER, "score_store", Map.of("store", "Allbirds")));

        assertThat(out.get("sustainabilityScore").asInt()).isEqualTo(88);
        assertThat(out.get("inYourTransactions").asBoolean()).isFalse();
        assertThat(out.get("flags").toString()).contains("b-corp");
    }

    @Test
    void unknownToolReturnsErrorInsteadOfThrowing() throws Exception {
        JsonNode out = mapper.readTree(toolsUnderTest.execute(USER, "no_such_tool", Map.of()));
        assertThat(out.get("error").asText()).contains("unknown tool");
    }
}
