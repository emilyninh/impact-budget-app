package com.impactbudget.categorization;

import com.impactbudget.common.TransactionIngested;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates impact scoring for a transaction:
 * <ol>
 *   <li>normalize the merchant string,</li>
 *   <li>look it up in the {@link MerchantScore} cache — a hit means no LLM call,</li>
 *   <li>on a miss, ask Claude, then overlay curated ground truth, and cache the result,</li>
 *   <li>persist the per-transaction {@link ImpactScore} and enqueue {@code TransactionScored}
 *       atomically via {@link ScoringPersistence} (transactional outbox).</li>
 * </ol>
 * The network-bound scoring runs outside any transaction; only the final DB write is
 * transactional.
 */
@Service
public class CategorizationService {

    private static final Logger log = LoggerFactory.getLogger(CategorizationService.class);

    private final MerchantScoreRepository merchantScoreRepository;
    private final CuratedOverrideService curatedOverrideService;
    private final MerchantScoringClient scoringClient;
    private final OpenFoodFactsEnricher openFoodFactsEnricher;
    private final WikidataLocalEnricher wikidataLocalEnricher;
    private final MerchantCategoryResolver categoryResolver;
    private final PlaidPfcMapper pfcMapper;
    private final WebsiteSignalEnricher websiteSignalEnricher;
    private final ScoringPersistence scoringPersistence;
    private final MeterRegistry meterRegistry;

    public CategorizationService(MerchantScoreRepository merchantScoreRepository,
                                 CuratedOverrideService curatedOverrideService,
                                 MerchantScoringClient scoringClient,
                                 OpenFoodFactsEnricher openFoodFactsEnricher,
                                 WikidataLocalEnricher wikidataLocalEnricher,
                                 MerchantCategoryResolver categoryResolver,
                                 PlaidPfcMapper pfcMapper,
                                 WebsiteSignalEnricher websiteSignalEnricher,
                                 ScoringPersistence scoringPersistence,
                                 MeterRegistry meterRegistry) {
        this.merchantScoreRepository = merchantScoreRepository;
        this.curatedOverrideService = curatedOverrideService;
        this.scoringClient = scoringClient;
        this.openFoodFactsEnricher = openFoodFactsEnricher;
        this.wikidataLocalEnricher = wikidataLocalEnricher;
        this.categoryResolver = categoryResolver;
        this.pfcMapper = pfcMapper;
        this.websiteSignalEnricher = websiteSignalEnricher;
        this.scoringPersistence = scoringPersistence;
        this.meterRegistry = meterRegistry;
    }

    public void categorize(TransactionIngested event) {
        String normalized = MerchantNormalizer.normalize(event.merchantRaw());
        MerchantScoring scoring = resolveMerchantScoring(
                normalized, event.merchantRaw(), event.sourceCategory(),
                event.merchantWebsite(), event.merchantName());
        String category = resolveCategory(event, scoring);

        scoringPersistence.persist(event, scoring, category);

        log.info("Scored txn {} [{}] category={} local={} sustainability={} source={}",
                event.transactionId(), normalized, category, scoring.localScore(),
                scoring.sustainabilityScore(), scoring.source());
    }

    /**
     * Final per-transaction category: Plaid's PFC (per-transaction, most accurate) wins; otherwise
     * fall back to the merchant-resolved category (curated/keyword/LLM, which is merchant-cached).
     * Category resolution lives here rather than in the merchant-score cache because PFC varies per
     * transaction while the cache is keyed only by merchant.
     */
    private String resolveCategory(TransactionIngested event, MerchantScoring scoring) {
        String fromPfc = pfcMapper.map(event.sourceCategory(), event.sourceCategoryDetailed());
        if (fromPfc != null) {
            return fromPfc;
        }
        // FOOD_AND_DRINK without Plaid's detailed split (historical): decide groceries vs eating out
        // from the merchant name, keeping food as the floor.
        if (PlaidPfcMapper.isFoodAndDrink(event.sourceCategory())) {
            String display = scoring.cleanedMerchant() != null ? scoring.cleanedMerchant() : event.merchantName();
            return categoryResolver.resolveFoodByMerchant(display);
        }
        return scoring.category();
    }

    /**
     * Re-run the full scoring chain for one merchant, discarding and refreshing its cache entry.
     * Used by the admin re-score to apply improved scoring (e.g. new website signals) to
     * already-loaded transactions. Returns the fresh scoring.
     */
    public MerchantScoring rescoreMerchant(String rawMerchant, String website, String merchantName) {
        String normalized = MerchantNormalizer.normalize(rawMerchant);
        merchantScoreRepository.findByNormalizedMerchant(normalized)
                .ifPresent(merchantScoreRepository::delete);
        String name = merchantName != null ? merchantName : rawMerchant;
        return resolveMerchantScoring(normalized, rawMerchant, null, website, name);
    }

    /** Cache-first resolution: reuse a cached score, else LLM + curated override, then cache it. */
    private MerchantScoring resolveMerchantScoring(String normalized, String rawMerchant,
                                                   String sourceCategory, String merchantWebsite,
                                                   String merchantName) {
        return merchantScoreRepository.findByNormalizedMerchant(normalized)
                .map(entity -> {
                    meterRegistry.counter("categorization.cache", "result", "hit").increment();
                    return fromEntity(entity);
                })
                .orElseGet(() -> {
                    meterRegistry.counter("categorization.cache", "result", "miss").increment();
                    // base scorer → Open Food Facts (sustainability) → Wikidata (local) →
                    // website signals (small-brand certifications) → curated (final authority).
                    MerchantScoring base = scoringClient.score(normalized, rawMerchant);
                    String display = base.cleanedMerchant() != null ? base.cleanedMerchant() : rawMerchant;
                    MerchantScoring withEco = openFoodFactsEnricher.enrich(display, base);
                    MerchantScoring withLocal = wikidataLocalEnricher.enrich(normalized, withEco);
                    String name = merchantName != null ? merchantName : display;
                    MerchantScoring withWeb =
                            websiteSignalEnricher.enrich(merchantWebsite, rawMerchant, name, withLocal);
                    MerchantScoring overridden = curatedOverrideService.apply(normalized, withWeb);
                    // Normalize the category onto the fixed taxonomy, falling back to the bank's
                    // own category as a hint when no scorer supplied one (e.g. imported data).
                    MerchantScoring resolved = withCategory(overridden, display, sourceCategory);
                    cache(normalized, resolved);
                    return resolved;
                });
    }

    /** Return a copy of the scoring with its category normalized onto the fixed taxonomy. */
    private MerchantScoring withCategory(MerchantScoring s, String display, String sourceCategory) {
        String hint = s.category() != null ? s.category() : sourceCategory;
        String category = categoryResolver.resolve(display, hint);
        return new MerchantScoring(s.cleanedMerchant(), category, s.localScore(),
                s.localIndependent(), s.sustainabilityScore(), s.materialFlags(),
                s.confidence(), s.rationale(), s.source());
    }

    private void cache(String normalized, MerchantScoring scoring) {
        MerchantScore ms = new MerchantScore();
        ms.setId(UUID.randomUUID());
        ms.setNormalizedMerchant(normalized);
        applyScoring(ms, scoring);
        try {
            merchantScoreRepository.save(ms);
        } catch (DataIntegrityViolationException race) {
            // Another thread cached the same merchant first — fine, its row stands.
            log.debug("Merchant '{}' was cached concurrently; keeping existing row", normalized);
        }
    }

    private void applyScoring(MerchantScore ms, MerchantScoring s) {
        ms.setCleanedMerchant(s.cleanedMerchant());
        ms.setCategory(s.category());
        ms.setLocalScore(s.localScore());
        ms.setLocalIndependent(s.localIndependent());
        ms.setSustainabilityScore(s.sustainabilityScore());
        ms.setMaterialFlags(joinFlags(s.materialFlags()));
        ms.setConfidence(s.confidence());
        ms.setRationale(s.rationale());
        ms.setSource(s.source());
    }

    private MerchantScoring fromEntity(MerchantScore ms) {
        return new MerchantScoring(
                ms.getCleanedMerchant(), ms.getCategory(), ms.getLocalScore(),
                ms.isLocalIndependent(), ms.getSustainabilityScore(),
                splitFlags(ms.getMaterialFlags()), ms.getConfidence(),
                ms.getRationale(), MerchantScoring.SOURCE_CACHE);
    }

    private static String joinFlags(List<String> flags) {
        return (flags == null || flags.isEmpty()) ? null : String.join(",", flags);
    }

    private static List<String> splitFlags(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }
}
