package com.impactbudget.categorization;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Normalizes messy bank merchant descriptors into a stable cache key.
 *
 * <p>Examples:
 * <pre>
 *   "TST*SQ*LOCAL COFFEE 12345"  -> "LOCAL COFFEE"
 *   "SQ *BLUE BOTTLE #0042"      -> "BLUE BOTTLE"
 *   "AMAZON.COM*A1B2C3"          -> "AMAZON"
 * </pre>
 */
public final class MerchantNormalizer {

    // Payment-processor / channel prefixes that carry no merchant identity.
    private static final Set<String> NOISE_TOKENS = Set.of(
            "TST", "SQ", "SP", "PY", "PP", "PAYPAL", "POS", "DEBIT", "CREDIT",
            "PURCHASE", "PMT", "PAYMENT", "WWW", "COM", "HTTP", "HTTPS", "INC", "LLC");

    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9 ]");
    private static final Pattern MULTISPACE = Pattern.compile("\\s+");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");

    private MerchantNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String upper = raw.toUpperCase();
        // Split processor markers (*, #, /) into spaces, then drop stray punctuation.
        upper = upper.replace('*', ' ').replace('#', ' ').replace('/', ' ');
        upper = NON_ALNUM.matcher(upper).replaceAll(" ");
        upper = MULTISPACE.matcher(upper).replaceAll(" ").trim();

        String cleaned = java.util.Arrays.stream(upper.split(" "))
                .filter(tok -> !tok.isBlank())
                .filter(tok -> !NOISE_TOKENS.contains(tok))     // drop processor noise
                .filter(tok -> !HAS_DIGIT.matcher(tok).matches()) // drop store ids / numbers
                .collect(Collectors.joining(" "))
                .trim();

        // If stripping removed everything (e.g. a numeric-only descriptor), fall back to
        // the punctuation-cleaned form so we still have a stable key.
        return cleaned.isBlank() ? upper : cleaned;
    }
}
