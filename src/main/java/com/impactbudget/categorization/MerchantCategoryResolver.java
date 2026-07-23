package com.impactbudget.categorization;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Normalizes a merchant into a fixed spending-category taxonomy so the dashboard can group
 * transactions consistently (Eating Out, Groceries, Shopping, …).
 *
 * <p>The LLM is asked to pick from the same taxonomy, but its output is free text and is null
 * when no scorer is running (the local demo). So this resolver is the backstop: it first tries
 * to map whatever the LLM said onto a taxonomy value, then falls back to a keyword match on the
 * merchant name, and finally to {@link #OTHER}. That keeps categories populated even with no LLM.
 */
@Component
public class MerchantCategoryResolver {

    public static final String EATING_OUT = "Eating Out";
    public static final String GROCERIES = "Groceries";
    public static final String SHOPPING = "Shopping";
    public static final String SUBSCRIPTIONS = "Subscriptions";
    public static final String TRANSPORT = "Transport";
    public static final String OTHER = "Other";

    /** Ordered keyword rules; the first matching category wins. */
    private static final List<Rule> RULES = List.of(
            new Rule(GROCERIES, "grocery", "groceries", "supermarket", "whole foods", "trader joe",
                    "walmart", "safeway", "kroger", "aldi", "costco", "farmers market", "market"),
            new Rule(EATING_OUT, "eating out", "restaurant", "dining", "food", "cafe", "coffee",
                    "starbucks", "mcdonald", "blue bottle", "rosie", "chipotle", "doordash",
                    "grubhub", "uber eats", "bar", "grill", "pizza", "chocolonely", "tea", "bakery",
                    "bagel", "nosh"),
            new Rule(SUBSCRIPTIONS, "subscription", "subscriptions", "netflix", "spotify", "hulu",
                    "disney+", "prime", "youtube", "membership", "saas", "entertainment", "kindle"),
            new Rule(TRANSPORT, "transport", "transportation", "uber", "lyft", "shell", "chevron",
                    "exxon", "gas", "fuel", "transit", "parking", "bart", "metro", "travel", "airfare",
                    "automotive", "airline", "airlines", "mta", "taxi"),
            new Rule(SHOPPING, "shopping", "retail", "apparel", "clothing", "amazon", "shein",
                    "h&m", "zara", "rei", "patagonia", "allbirds", "bombas", "klean kanteen",
                    "target", "store", "outfitters", "merchandise", "pharmacy", "cvs", "walgreens"));

    /**
     * Resolve to a taxonomy value.
     *
     * @param merchantName cleaned merchant display name (may be null)
     * @param llmCategory  the free-text category the LLM returned (may be null)
     */
    public String resolve(String merchantName, String llmCategory) {
        String fromLlm = match(llmCategory);
        if (fromLlm != null) {
            return fromLlm;
        }
        String fromName = match(merchantName);
        if (fromName != null) {
            return fromName;
        }
        return OTHER;
    }

    private String match(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String haystack = text.toLowerCase(Locale.ROOT);
        for (Rule rule : RULES) {
            for (String keyword : rule.keywords()) {
                if (haystack.contains(keyword)) {
                    return rule.category();
                }
            }
        }
        return null;
    }

    private record Rule(String category, String... keywords) {
    }
}
