package com.impactbudget.categorization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Raises the sustainability (and, for independents, local) score of lesser-known small merchants by
 * reading their own website — free and key-less. Resolves a domain ({@link DomainResolver}), fetches
 * a few pages, and detects concrete certifications/materials (GOTS, OEKO-TEX, organic, Fair Trade, B
 * Corp, recycled, 1% for the Planet, …). Each detected signal raises the score and adds an
 * explainable flag, so a boosted score always cites a real credential ("show your work").
 *
 * <p>Only-raise + flag-union, mirroring {@link OpenFoodFactsEnricher}; curated overrides still win.
 * Runs only for plausibly-independent merchants (skips Wikidata-demoted multinationals) and degrades
 * to the base scoring on any miss or error. Rides the {@code merchant_score} cache, so a merchant's
 * site is fetched at most once.
 */
@Component
public class WebsiteSignalEnricher {

    private static final Logger log = LoggerFactory.getLogger(WebsiteSignalEnricher.class);

    /** Same-domain pages likely to state sustainability practices, tried in order. */
    private static final List<String> PATHS = List.of(
            "/", "/about", "/about-us", "/pages/about", "/pages/why-us",
            "/pages/our-story", "/sustainability", "/pages/sustainability", "/pages/materials");

    /** Certification / material signals → (weight, flag). First keyword hit counts once per signal. */
    private static final List<Signal> SIGNALS = List.of(
            new Signal(25, "gots-certified", "gots", "global organic textile standard"),
            new Signal(20, "b-corp", "b corp", "b corporation", "certified b"),
            new Signal(20, "organic", "organic cotton", "organic silk", "organic linen",
                    "organic wool", "certified organic", "gots", "organic"),
            new Signal(15, "oeko-tex", "oeko-tex", "oekotex"),
            new Signal(15, "recycled", "recycled", "grs certified", "global recycled standard", "rpet"),
            new Signal(15, "fair-trade", "fair trade", "fairtrade"),
            new Signal(15, "1%-for-planet", "1% for the planet", "one percent for the planet"),
            new Signal(15, "carbon-neutral", "carbon neutral", "climate neutral", "carbon-neutral"),
            new Signal(12, "regenerative", "regenerative"),
            new Signal(10, "fsc", "forest stewardship", "fsc certified", "fsc-certified"),
            new Signal(10, "natural-fiber", "mulberry silk", "hemp", "tencel", "lyocell",
                    "linen", "merino"),
            new Signal(10, "plastic-free", "plastic-free", "plastic free", "compostable",
                    "biodegradable", "plastic neutral"),
            new Signal(8, "vegan", "vegan", "cruelty-free", "cruelty free"));

    private static final String[] INDEPENDENT_MARKERS = {
            "cdn.shopify.com", "myshopify", "powered by shopify", "family-owned", "family owned",
            "independently owned", "small business", "small-batch", "small batch", "handmade",
            "woman-owned", "women-owned", "founded in"};

    private final WebSignalProperties props;
    private final ScoringProperties scoringProps;
    private final OllamaProperties ollamaProps;
    private final ObjectMapper mapper;
    private final MeterRegistry meterRegistry;
    private final RestClient ollama;   // optional, for the one-line rationale ("Both" mode)

    public WebsiteSignalEnricher(WebSignalProperties props, ScoringProperties scoringProps,
                                 OllamaProperties ollamaProps, ObjectMapper mapper,
                                 MeterRegistry meterRegistry) {
        this.props = props;
        this.scoringProps = scoringProps;
        this.ollamaProps = ollamaProps;
        this.mapper = mapper;
        this.meterRegistry = meterRegistry;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.ollama = RestClient.builder().baseUrl(ollamaProps.baseUrlOrDefault())
                .requestFactory(factory).build();
    }

    /**
     * @param plaidWebsite  Plaid's merchant website, if captured (may be null)
     * @param rawDescriptor the raw bank descriptor (may carry a domain or the Shopify "SP " marker)
     * @param merchantName  cleaned merchant display name (used to guess/verify a domain)
     */
    public MerchantScoring enrich(String plaidWebsite, String rawDescriptor, String merchantName,
                                  MerchantScoring base) {
        // Skip when disabled, or when the merchant is already known to be a multinational (Wikidata
        // demoted its local score) — no point fetching Shein's site.
        if (!props.enabledOrDefault() || base.localScore() < 30) {
            return base;
        }
        DomainResolver.Candidate candidate = DomainResolver.resolve(plaidWebsite, rawDescriptor, merchantName);
        if (candidate == null) {
            return miss(base);
        }
        try {
            String content = fetch(candidate.domain());
            if (content == null || content.isBlank()) {
                return miss(base);
            }
            // A guessed domain must actually be this brand's site, or we drop it.
            if (candidate.needsVerification() && !mentionsBrand(content, merchantName)) {
                log.debug("Guessed domain {} did not verify against '{}'", candidate.domain(), merchantName);
                return miss(base);
            }

            Scan scan = scanSignals(content);
            Set<String> flags = new LinkedHashSet<>(base.materialFlags());
            flags.addAll(scan.flags());
            boolean independent = Arrays.stream(INDEPENDENT_MARKERS).anyMatch(content::contains)
                    || isShopifyDescriptor(rawDescriptor);

            if (scan.weight() == 0 && !independent) {
                return miss(base);   // fetched, but nothing to say — leave the base untouched
            }
            meterRegistry.counter("categorization.web-signal", "result", "hit").increment();

            int sustainability = scan.weight() > 0
                    ? Math.max(base.sustainabilityScore(), Math.min(95, 55 + scan.weight()))
                    : base.sustainabilityScore();

            int localScore = base.localScore();
            boolean localIndependent = base.localIndependent();
            // Raise local only for an untouched-neutral merchant (Wikidata didn't demote, and the
            // base scorer isn't already confidently local).
            if (independent && base.localScore() >= 30 && base.localScore() < 60) {
                localScore = Math.max(base.localScore(), 80);
                localIndependent = true;
            }

            String source = MerchantScoring.SOURCE_FALLBACK.equals(base.source())
                    ? MerchantScoring.SOURCE_WEB : base.source();
            String rationale = rationale(candidate.domain(), flags, content, base);
            // Prefer the real merchant name over the raw descriptor for display (the base fallback
            // leaves cleanedMerchant = the raw string like "SP LINA LENNOX").
            String cleaned = StringUtils.hasText(merchantName) ? merchantName : base.cleanedMerchant();

            return new MerchantScoring(cleaned, base.category(),
                    localScore, localIndependent, sustainability, List.copyOf(flags),
                    Math.max(base.confidence(), 0.7), rationale, source);
        } catch (Exception e) {
            log.debug("Website signal enrichment failed for '{}' ({})", candidate.domain(), e.toString());
            return base;
        }
    }

    /** Fetch up to maxPages same-domain pages and return the combined lowercased HTML, or null. */
    private String fetch(String domain) {
        StringBuilder sb = new StringBuilder();
        int fetched = 0;
        boolean homeOk = false;
        for (String path : PATHS) {
            if (fetched >= props.maxPagesOrDefault()) {
                break;
            }
            try {
                Document doc = Jsoup.connect("https://" + domain + path)
                        .userAgent("ImpactBudget/0.1 (+sustainability scoring)")
                        .timeout(props.timeoutMsOrDefault())
                        .followRedirects(true)
                        .get();
                sb.append(doc.html().toLowerCase(Locale.ROOT)).append('\n');
                fetched++;
                if (path.equals("/")) {
                    homeOk = true;
                }
            } catch (Exception perPage) {
                if (path.equals("/")) {
                    return null;   // homepage unreachable → treat the whole site as a miss
                }
                // other paths are optional (404s are expected) — keep going
            }
        }
        // Cap the scanned text so a huge site can't blow up memory.
        String all = sb.toString();
        return homeOk ? (all.length() > 400_000 ? all.substring(0, 400_000) : all) : null;
    }

    /** All name tokens (len ≥ 3) appear in the page → it's plausibly this brand. */
    private boolean mentionsBrand(String content, String merchantName) {
        if (!StringUtils.hasText(merchantName)) {
            return false;
        }
        String[] tokens = merchantName.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        boolean any = false;
        for (String token : tokens) {
            if (token.length() >= 3) {
                any = true;
                if (!content.contains(token)) {
                    return false;
                }
            }
        }
        return any;
    }

    /** Deterministic rationale citing the detected credentials; upgraded to an Ollama one-liner. */
    private String rationale(String domain, Set<String> flags, String content, MerchantScoring base) {
        String detected = flags.isEmpty() ? "independent-business signals" : String.join(", ", flags);
        String deterministic = "Website signals on " + domain + ": " + detected;
        if (!"ollama".equalsIgnoreCase(scoringProps.providerOrDefault())) {
            return deterministic;
        }
        try {
            String prompt = "In one short sentence, summarize the sustainability of the brand at "
                    + domain + " based on these detected signals: " + detected
                    + ". Be factual, no marketing language.";
            String body = mapper.writeValueAsString(java.util.Map.of(
                    "model", ollamaProps.modelOrDefault(), "prompt", prompt, "stream", false));
            String resp = ollama.post().uri("/api/generate")
                    .header("Content-Type", "application/json").body(body)
                    .retrieve().body(String.class);
            JsonNode node = mapper.readTree(resp);
            String text = node.path("response").asText("").trim();
            return StringUtils.hasText(text) ? text : deterministic;
        } catch (Exception e) {
            return deterministic;   // Ollama not running / failed — deterministic is fine
        }
    }

    /** Detected sustainability signals in page content: total weight + the flags to attach. */
    record Scan(int weight, Set<String> flags) {
    }

    /** Scan already-lowercased page content against the signal table. Package-private for tests. */
    static Scan scanSignals(String content) {
        Set<String> flags = new LinkedHashSet<>();
        int weight = 0;
        for (Signal s : SIGNALS) {
            if (s.matches(content)) {
                flags.add(s.flag());
                weight += s.weight();
            }
        }
        return new Scan(weight, flags);
    }

    /** Shopify Payments descriptors ("SP …") mark a small independent online merchant. */
    private static boolean isShopifyDescriptor(String descriptor) {
        if (!StringUtils.hasText(descriptor)) {
            return false;
        }
        String d = descriptor.trim().toUpperCase(Locale.ROOT);
        return d.startsWith("SP ") || d.startsWith("SP*");
    }

    private MerchantScoring miss(MerchantScoring base) {
        meterRegistry.counter("categorization.web-signal", "result", "miss").increment();
        return base;
    }

    /** A sustainability signal: any of its keywords in the page contributes its weight + flag. */
    private record Signal(int weight, String flag, String... keywords) {
        boolean matches(String content) {
            for (String k : keywords) {
                if (content.contains(k)) {
                    return true;
                }
            }
            return false;
        }
    }
}
