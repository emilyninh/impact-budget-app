package com.impactbudget.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impactbudget.budget.BudgetAggregate;
import com.impactbudget.budget.BudgetAggregateService;
import com.impactbudget.budget.BudgetStatus;
import com.impactbudget.budget.CategoryBreakdown;
import com.impactbudget.categorization.CategorizationService;
import com.impactbudget.categorization.GreenerAlternative;
import com.impactbudget.categorization.MerchantNormalizer;
import com.impactbudget.categorization.MerchantScore;
import com.impactbudget.categorization.MerchantScoreRepository;
import com.impactbudget.categorization.MerchantScoring;
import com.impactbudget.categorization.SwapSuggestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The grounded "tool layer" the assistant calls: each method reads real data from the existing
 * dashboard/scoring services so the model answers from actual numbers instead of inventing them.
 *
 * <p>Deliberately free of any LLM SDK types — it exposes plain {@link Spec} descriptors (which
 * {@code AssistantService} translates into provider tools) and a single {@link #execute} dispatch
 * that returns a JSON string. That keeps this class unit-testable without a network or an API key.
 */
@Component
public class AssistantTools {

    private static final Logger log = LoggerFactory.getLogger(AssistantTools.class);

    /** A tool's callable name, its description, and its parameters (for the provider schema). */
    public record Spec(String name, String description, List<Param> params) {
    }

    /** One tool parameter — always a string here; {@code required} drives the schema. */
    public record Param(String name, String description, boolean required) {
    }

    private final SpendReader spend;                 // small seam so tests can stub budget status
    private final BudgetAggregateService aggregates;
    private final CategorizationService categorization;
    private final MerchantScoreRepository merchantScores;
    private final SwapSuggestionService swaps;
    private final ObjectMapper mapper;

    @org.springframework.beans.factory.annotation.Autowired
    public AssistantTools(com.impactbudget.budget.SpendBudgetService spendBudgetService,
                          BudgetAggregateService aggregates,
                          CategorizationService categorization,
                          MerchantScoreRepository merchantScores,
                          SwapSuggestionService swaps,
                          ObjectMapper mapper) {
        this(spendBudgetService::status, aggregates, categorization, merchantScores, swaps, mapper);
    }

    /** Test seam: inject the budget-status function directly. */
    AssistantTools(SpendReader spend, BudgetAggregateService aggregates,
                   CategorizationService categorization, MerchantScoreRepository merchantScores,
                   SwapSuggestionService swaps, ObjectMapper mapper) {
        this.spend = spend;
        this.aggregates = aggregates;
        this.categorization = categorization;
        this.merchantScores = merchantScores;
        this.swaps = swaps;
        this.mapper = mapper;
    }

    /** Narrow view of {@code SpendBudgetService.status} so tests don't need the whole service. */
    @FunctionalInterface
    interface SpendReader {
        BudgetStatus status(String userId, String yearMonth);
    }

    /** The tools advertised to the model. Categories referenced here are the fixed taxonomy names. */
    public List<Spec> specs() {
        return List.of(
                new Spec("get_budget_status",
                        "The user's spending vs. their monthly budget for the current month: limit, "
                                + "spent, remaining, whether they are over budget and by how much.",
                        List.of()),
                new Spec("get_month_summary",
                        "This month's impact summary: total spend, transaction count, "
                                + "confidence-weighted local %% and sustainability %%, dollars at "
                                + "local & independent businesses, and scored-coverage %%.",
                        List.of()),
                new Spec("get_category_spend",
                        "Spending broken down by category for the current month. Pass a category "
                                + "name (e.g. Shopping, Groceries, Eating Out) to get just that one, "
                                + "or omit it to get every category.",
                        List.of(new Param("category", "Category name to filter to, or omit for all",
                                false))),
                new Spec("score_store",
                        "Impact scores for a specific store/brand: local score, sustainability "
                                + "score, whether it is local & independent, and the credentials "
                                + "behind the score. Works for any store, scored live if unknown.",
                        List.of(new Param("store", "The store or brand name, e.g. Patagonia", true))),
                new Spec("get_greener_alternatives",
                        "Higher-sustainability merchants in a category, as swap suggestions.",
                        List.of(new Param("category", "Category name, e.g. Shopping", true))));
    }

    /**
     * Run a tool and return its result as a JSON string (never throws — a failure is reported as an
     * {@code error} field so the agent loop can continue and tell the user what went wrong).
     */
    public String execute(String userId, String toolName, Map<String, Object> args) {
        try {
            return switch (toolName) {
                case "get_budget_status" -> json(budgetStatus(userId));
                case "get_month_summary" -> json(monthSummary(userId));
                case "get_category_spend" -> json(categorySpend(userId, str(args, "category")));
                case "score_store" -> json(scoreStore(str(args, "store")));
                case "get_greener_alternatives" -> json(greenerAlternatives(str(args, "category")));
                default -> json(Map.of("error", "unknown tool: " + toolName));
            };
        } catch (Exception e) {
            log.warn("Assistant tool '{}' failed: {}", toolName, e.toString());
            return json(Map.of("error", "tool '" + toolName + "' failed: " + e.getMessage()));
        }
    }

    private Map<String, Object> budgetStatus(String userId) {
        BudgetStatus s = spend.status(userId, month());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("month", s.yearMonth());
        out.put("monthlyLimit", s.monthlyLimit());
        out.put("spent", s.spent());
        out.put("remaining", s.remaining());
        boolean over = s.status() == BudgetStatus.Status.OVER;
        out.put("overBudget", over);
        if (over && s.remaining() != null) {
            out.put("amountOver", s.remaining().negate());
        }
        out.put("pctUsed", s.pctUsed());
        out.put("status", s.status().name());
        out.put("note", "projectedSpend is a naive day-of-month extrapolation and is unreliable early "
                + "in the month; prefer 'spent' and 'remaining'.");
        return out;
    }

    private Map<String, Object> monthSummary(String userId) {
        BudgetAggregate a = aggregates.getMonthly(userId, month());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("month", a.yearMonth());
        out.put("totalSpend", a.totalSpend());
        out.put("transactionCount", a.transactionCount());
        out.put("localImpactPct", a.localImpactPct());
        out.put("sustainabilityImpactPct", a.sustainabilityImpactPct());
        out.put("localIndependentSpend", a.localIndependentSpend());
        out.put("scoredSharePct", a.scoredSharePct());
        return out;
    }

    private Map<String, Object> categorySpend(String userId, String category) {
        List<CategoryBreakdown> all = aggregates.categoryBreakdown(userId, month());
        List<Map<String, Object>> rows = all.stream()
                .filter(c -> category == null || category.isBlank()
                        || c.category().equalsIgnoreCase(category.trim()))
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("category", c.category());
                    m.put("totalSpend", c.totalSpend());
                    m.put("txnCount", c.txnCount());
                    m.put("avgSustainability", c.avgSustainability());
                    return m;
                })
                .toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("month", month());
        if (category != null && !category.isBlank()) {
            out.put("requestedCategory", category);
            if (rows.isEmpty()) {
                out.put("note", "No spending in that category this month.");
            }
        }
        out.put("categories", rows);
        return out;
    }

    private Map<String, Object> scoreStore(String store) {
        if (store == null || store.isBlank()) {
            return Map.of("error", "no store name provided");
        }
        String normalized = MerchantNormalizer.normalize(store);
        MerchantScore cached = merchantScores.findByNormalizedMerchant(normalized).orElse(null);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("store", store);
        if (cached != null) {
            out.put("localScore", cached.getLocalScore());
            out.put("sustainabilityScore", cached.getSustainabilityScore());
            out.put("localIndependent", cached.isLocalIndependent());
            out.put("flags", splitFlags(cached.getMaterialFlags()));
            out.put("rationale", cached.getRationale());
            out.put("source", cached.getSource());
            out.put("confidence", cached.getConfidence());
            out.put("inYourTransactions", true);
        } else {
            // Not in the data — score it live through the full pipeline (curated → website signals).
            MerchantScoring s = categorization.rescoreMerchant(store, null, null, store);
            out.put("localScore", s.localScore());
            out.put("sustainabilityScore", s.sustainabilityScore());
            out.put("localIndependent", s.localIndependent());
            out.put("flags", s.materialFlags());
            out.put("rationale", s.rationale());
            out.put("source", s.source());
            out.put("confidence", s.confidence());
            out.put("inYourTransactions", false);
        }
        return out;
    }

    private Map<String, Object> greenerAlternatives(String category) {
        List<GreenerAlternative> alts = swaps.greenerAlternatives(category, 70, null, 3);
        List<Map<String, Object>> rows = alts.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("merchant", a.merchant());
            m.put("sustainabilityScore", a.sustainabilityScore());
            m.put("flags", a.flags());
            return m;
        }).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("category", category);
        out.put("alternatives", rows);
        if (rows.isEmpty()) {
            out.put("note", "No higher-sustainability merchants known in that category yet.");
        }
        return out;
    }

    private static String month() {
        return YearMonth.now().toString();
    }

    private static String str(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        return v == null ? null : v.toString();
    }

    private static List<String> splitFlags(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{\"error\":\"could not serialize tool result\"}";
        }
    }
}
