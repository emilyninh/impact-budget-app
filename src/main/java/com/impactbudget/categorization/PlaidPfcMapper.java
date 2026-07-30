package com.impactbudget.categorization;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Maps Plaid's Personal Finance Category (PFC) onto the app's spending taxonomy. Plaid already
 * classifies transactions into a mature {@code primary}/{@code detailed} scheme that natively
 * separates flights (TRAVEL), rent (RENT_AND_UTILITIES), transfers (TRANSFER_IN/OUT), etc., so this
 * is the authoritative category source for Plaid-linked transactions — far better than merchant
 * keyword matching. Returns {@code null} when the PFC is absent or doesn't map to a meaningful
 * bucket, so the caller can fall back to {@link MerchantCategoryResolver}.
 *
 * <p>Reference: Plaid's PFC taxonomy (primary is one of ~16 constants; detailed is
 * {@code PRIMARY_SUFFIX}). We match on primary and refine with detailed only where the split
 * matters (Groceries vs Eating Out, Rent vs Utilities, Mortgage vs other loans, streaming subs).
 */
@Component
public class PlaidPfcMapper {

    /**
     * @param primary  Plaid PFC primary (e.g. {@code FOOD_AND_DRINK}); may be null
     * @param detailed Plaid PFC detailed (e.g. {@code FOOD_AND_DRINK_GROCERIES}); may be null
     * @return a taxonomy value from {@link MerchantCategoryResolver}, or null if unmapped
     */
    /** Whether the PFC primary is FOOD_AND_DRINK — used to route the detailed-less grocery split. */
    public static boolean isFoodAndDrink(String primary) {
        return primary != null && "FOOD_AND_DRINK".equalsIgnoreCase(primary.trim());
    }

    public String map(String primary, String detailed) {
        if (primary == null || primary.isBlank()) {
            return null;
        }
        String p = primary.toUpperCase(Locale.ROOT).trim();
        String d = detailed == null ? "" : detailed.toUpperCase(Locale.ROOT).trim();

        return switch (p) {
            case "INCOME" -> MerchantCategoryResolver.INCOME;
            case "TRANSFER_IN", "TRANSFER_OUT" -> MerchantCategoryResolver.TRANSFERS;
            case "TRANSPORTATION" -> MerchantCategoryResolver.TRANSPORT;
            case "TRAVEL" -> MerchantCategoryResolver.TRAVEL;
            case "GENERAL_MERCHANDISE" -> MerchantCategoryResolver.SHOPPING;
            case "MEDICAL", "PERSONAL_CARE" -> MerchantCategoryResolver.HEALTH;
            case "HOME_IMPROVEMENT" -> MerchantCategoryResolver.HOUSING;
            case "BANK_FEES" -> MerchantCategoryResolver.BILLS;
            case "FOOD_AND_DRINK" ->
                    // Groceries when Plaid's detailed says so; Eating Out when detailed is present
                    // but non-grocery; null when detailed is absent (historical rows) so the caller
                    // can split by merchant name instead of defaulting everything to Eating Out.
                    d.contains("GROCERIES") ? MerchantCategoryResolver.GROCERIES
                            : d.isBlank() ? null
                            : MerchantCategoryResolver.EATING_OUT;
            case "RENT_AND_UTILITIES" ->
                    // Detailed is PRIMARY_SUFFIX, so the "RENT_AND_UTILITIES" prefix always contains
                    // "RENT" — match the rent *suffix* specifically, else it's a utility bill.
                    d.endsWith("_RENT") ? MerchantCategoryResolver.HOUSING
                            : MerchantCategoryResolver.BILLS;
            case "LOAN_PAYMENTS" ->
                    d.contains("MORTGAGE") ? MerchantCategoryResolver.HOUSING
                            : MerchantCategoryResolver.BILLS;
            case "ENTERTAINMENT" ->
                    // Streaming/music read as recurring subscriptions; the rest is entertainment.
                    (d.contains("TV_AND_MOVIES") || d.contains("MUSIC_AND_AUDIO"))
                            ? MerchantCategoryResolver.SUBSCRIPTIONS
                            : MerchantCategoryResolver.ENTERTAINMENT;
            case "GENERAL_SERVICES" ->
                    d.contains("INSURANCE") ? MerchantCategoryResolver.BILLS : null;
            case "GOVERNMENT_AND_NON_PROFIT" -> MerchantCategoryResolver.OTHER;
            default -> null;   // unknown primary → let the keyword/LLM fallback decide
        };
    }
}
