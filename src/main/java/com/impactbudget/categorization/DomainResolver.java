package com.impactbudget.categorization;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves a merchant to a website domain for web-signal scoring, in order of trust:
 * <ol>
 *   <li>Plaid's merchant website (authoritative — no verification needed),</li>
 *   <li>a domain embedded in the raw bank descriptor (e.g. {@code WWW.SIMPLEECOLOGY.COM}),</li>
 *   <li>a <em>guess</em> from the merchant name for online/Shopify merchants (e.g. "Lina Lennox" →
 *       {@code linalennox.com}) — the caller must verify the fetched page matches the brand.</li>
 * </ol>
 */
final class DomainResolver {

    /** A resolved domain and whether the caller must verify it belongs to the brand. */
    record Candidate(String domain, boolean needsVerification) {
    }

    // A bare domain like simpleecology.com or try-suri.co, with a small set of common TLDs.
    private static final Pattern DOMAIN = Pattern.compile(
            "([a-z0-9][a-z0-9-]*\\.)+(com|co|net|org|shop|store|us|io)",
            Pattern.CASE_INSENSITIVE);
    // Shopify Payments descriptors start with "SP " — a strong small-independent-merchant marker.
    private static final Pattern SHOPIFY_PREFIX = Pattern.compile("^SP[\\s*]", Pattern.CASE_INSENSITIVE);

    private DomainResolver() {
    }

    static Candidate resolve(String plaidWebsite, String rawDescriptor, String merchantName) {
        if (StringUtils.hasText(plaidWebsite)) {
            return new Candidate(normalize(plaidWebsite), false);
        }
        String fromDescriptor = extractDomain(rawDescriptor);
        if (fromDescriptor != null) {
            return new Candidate(fromDescriptor, false);
        }
        // No domain anywhere — only guess for online-looking (Shopify) merchants with a name.
        if (looksOnline(rawDescriptor) && StringUtils.hasText(merchantName)) {
            String slug = slug(merchantName);
            if (slug.length() >= 3) {
                return new Candidate(slug + ".com", true);
            }
        }
        return null;
    }

    /** Extract a bare domain from arbitrary text (bank descriptor), or null. */
    static String extractDomain(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher m = DOMAIN.matcher(text);
        return m.find() ? normalize(m.group()) : null;
    }

    /** Strip scheme/www/path and lowercase → bare registrable domain. */
    static String normalize(String raw) {
        String d = raw.trim().toLowerCase(Locale.ROOT);
        d = d.replaceFirst("^https?://", "");
        d = d.replaceFirst("^www\\.", "");
        int slash = d.indexOf('/');
        if (slash >= 0) {
            d = d.substring(0, slash);
        }
        return d;
    }

    private static boolean looksOnline(String descriptor) {
        return descriptor != null && SHOPIFY_PREFIX.matcher(descriptor.trim()).find();
    }

    private static String slug(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
