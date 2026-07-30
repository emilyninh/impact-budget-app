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
    public static final String TRAVEL = "Travel";
    public static final String HOUSING = "Housing & Rent";
    public static final String BILLS = "Bills & Utilities";
    public static final String HEALTH = "Health";
    public static final String ENTERTAINMENT = "Entertainment";
    public static final String TRANSFERS = "Transfers";
    public static final String INCOME = "Income";
    public static final String OTHER = "Other";

    /**
     * Ordered keyword rules; the first matching category wins. This is the fallback used when
     * Plaid's personal-finance category isn't available (CSV imports, demo data) — Plaid-linked
     * transactions are categorized from PFC first (see {@link PlaidPfcMapper}). Order is
     * deliberate: specific categories precede the generic {@link #SHOPPING} catch-all, and the
     * taxonomy labels themselves match here so an LLM's category hint round-trips.
     */
    private static final List<Rule> RULES = List.of(
            new Rule(TRANSFERS, "transfer", "wire transfer", "zelle", "withdrawal", "atm ",
                    "brokerage", "robinhood", "e*trade", "etrade", "coinbase",
                    // Money moved to brokerages/banks reads as a transfer, not spending. These
                    // descriptors show up on real statements (e.g. Fidelity's "FID BKG SVC …
                    // MONEYLINE" ACH pulls) but aren't tagged TRANSFER_OUT by Plaid.
                    "moneyline", "fid bkg", "fidelity", "vanguard", "schwab", "wealthfront",
                    "betterment", "ach transfer", "online transfer"),
            new Rule(INCOME, "income", "payroll", "direct deposit", "salary", "paycheck",
                    "dividend", "interest paid"),
            new Rule(GROCERIES, "grocery", "groceries", "supermarket", "whole foods", "trader joe",
                    "walmart", "safeway", "kroger", "aldi", "costco", "farmers market", "market"),
            new Rule(EATING_OUT, "eating out", "restaurant", "dining", "food", "cafe", "coffee",
                    "starbucks", "mcdonald", "blue bottle", "rosie", "chipotle", "doordash",
                    "grubhub", "uber eats", "bar", "grill", "pizza", "chocolonely", "tea", "bakery",
                    "bagel", "nosh"),
            new Rule(TRAVEL, "travel", "airfare", "airline", "airlines", "flight", "delta air",
                    "united air", "southwest", "jetblue", "hotel", "motel", "airbnb", "expedia",
                    "marriott", "hilton", "resort", "booking.com", "lodging"),
            new Rule(TRANSPORT, "transport", "transportation", "uber", "lyft", "shell", "chevron",
                    "exxon", "gas station", "fuel", "transit", "parking", "bart", "metro",
                    "automotive", "mta", "taxi"),
            new Rule(HOUSING, "rent", "landlord", "mortgage", "apartment", "property management",
                    "hoa", "home depot", "lowe's", "hardware", "furniture"),
            new Rule(BILLS, "bill", "utility", "utilities", "electric", "internet", "comcast",
                    "xfinity", "verizon", "at&t", "t-mobile", "insurance", "geico", "loan",
                    "pg&e", "phone"),
            new Rule(HEALTH, "health", "pharmacy", "cvs", "walgreens", "doctor", "dental",
                    "dentist", "clinic", "hospital", "medical", "optometry", "therapy"),
            new Rule(SUBSCRIPTIONS, "subscription", "subscriptions", "netflix", "spotify", "hulu",
                    "disney+", "prime", "youtube", "membership", "saas", "kindle", "patreon",
                    "icloud", "dropbox"),
            new Rule(ENTERTAINMENT, "entertainment", "cinema", "movie", "theatre", "theater",
                    "concert", "ticketmaster", "stubhub", "amc", "fandango", "steam", "playstation",
                    "xbox", "nintendo", "arcade", "museum"),
            new Rule(SHOPPING, "shopping", "retail", "apparel", "clothing", "amazon", "shein",
                    "h&m", "zara", "rei", "patagonia", "allbirds", "bombas", "klean kanteen",
                    "target", "store", "outfitters", "merchandise"));

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

    /**
     * Split a food transaction into Groceries vs Eating Out using the merchant name alone. Used for
     * Plaid FOOD_AND_DRINK rows that lack the detailed category (historical data): a grocery-keyword
     * match (Trader Joe's, Whole Foods, …) is Groceries; everything else keeps the food default of
     * Eating Out. Deliberately ignores any category hint so the merchant name is the sole signal.
     */
    public String resolveFoodByMerchant(String merchantName) {
        String haystack = merchantName == null ? "" : merchantName.toLowerCase(Locale.ROOT);
        for (Rule rule : RULES) {
            if (rule.category().equals(GROCERIES)) {
                for (String keyword : rule.keywords()) {
                    if (haystack.contains(keyword)) {
                        return GROCERIES;
                    }
                }
            }
        }
        return EATING_OUT;
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
