package com.impactbudget.categorization;

import com.impactbudget.common.TransactionIngested;
import com.impactbudget.common.TransactionScored;
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
 *   <li>write the per-transaction {@link ImpactScore} (idempotent) and publish
 *       {@link TransactionScored}.</li>
 * </ol>
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
    private final ImpactScoreRepository impactScoreRepository;
    private final TransactionScoredPublisher publisher;
    private final MeterRegistry meterRegistry;

    public CategorizationService(MerchantScoreRepository merchantScoreRepository,
                                 CuratedOverrideService curatedOverrideService,
                                 MerchantScoringClient scoringClient,
                                 OpenFoodFactsEnricher openFoodFactsEnricher,
                                 WikidataLocalEnricher wikidataLocalEnricher,
                                 MerchantCategoryResolver categoryResolver,
                                 ImpactScoreRepository impactScoreRepository,
                                 TransactionScoredPublisher publisher,
                                 MeterRegistry meterRegistry) {
        this.merchantScoreRepository = merchantScoreRepository;
        this.curatedOverrideService = curatedOverrideService;
        this.scoringClient = scoringClient;
        this.openFoodFactsEnricher = openFoodFactsEnricher;
        this.wikidataLocalEnricher = wikidataLocalEnricher;
        this.categoryResolver = categoryResolver;
        this.impactScoreRepository = impactScoreRepository;
        this.publisher = publisher;
        this.meterRegistry = meterRegistry;
    }

    public void categorize(TransactionIngested event) {
        String normalized = MerchantNormalizer.normalize(event.merchantRaw());
        MerchantScoring scoring = resolveMerchantScoring(normalized, event.merchantRaw());

        saveImpactScore(event, scoring);
        publisher.publishScored(new TransactionScored(
                event.transactionId(), event.userId(), displayMerchant(scoring, event), event.amount(),
                event.txnDate(), scoring.category(), scoring.localScore(), scoring.localIndependent(),
                scoring.sustainabilityScore(), scoring.materialFlags(),
                scoring.confidence(), scoring.source()));

        log.info("Scored txn {} [{}] local={} sustainability={} source={}",
                event.transactionId(), normalized, scoring.localScore(),
                scoring.sustainabilityScore(), scoring.source());
    }

    /** Cache-first resolution: reuse a cached score, else LLM + curated override, then cache it. */
    private MerchantScoring resolveMerchantScoring(String normalized, String rawMerchant) {
        return merchantScoreRepository.findByNormalizedMerchant(normalized)
                .map(entity -> {
                    meterRegistry.counter("categorization.cache", "result", "hit").increment();
                    return fromEntity(entity);
                })
                .orElseGet(() -> {
                    meterRegistry.counter("categorization.cache", "result", "miss").increment();
                    // base scorer → Open Food Facts (sustainability) → Wikidata (local) →
                    // curated override (final authority, wins on conflict).
                    MerchantScoring base = scoringClient.score(normalized, rawMerchant);
                    String display = base.cleanedMerchant() != null ? base.cleanedMerchant() : rawMerchant;
                    MerchantScoring withEco = openFoodFactsEnricher.enrich(display, base);
                    MerchantScoring withLocal = wikidataLocalEnricher.enrich(normalized, withEco);
                    MerchantScoring overridden = curatedOverrideService.apply(normalized, withLocal);
                    // Normalize the (possibly free-text / null) category onto the fixed taxonomy,
                    // so both the cache and the published event carry a clean category value.
                    MerchantScoring resolved = withCategory(overridden, display);
                    cache(normalized, resolved);
                    return resolved;
                });
    }

    /** Return a copy of the scoring with its category normalized onto the fixed taxonomy. */
    private MerchantScoring withCategory(MerchantScoring s, String display) {
        String category = categoryResolver.resolve(display, s.category());
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

    private void saveImpactScore(TransactionIngested event, MerchantScoring s) {
        ImpactScore score = impactScoreRepository.findByTransactionId(event.transactionId())
                .orElseGet(ImpactScore::new);
        if (score.getId() == null) {
            score.setId(UUID.randomUUID());
            score.setTransactionId(event.transactionId());
        }
        score.setUserId(event.userId());
        score.setCategory(s.category());
        score.setLocalScore(s.localScore());
        score.setLocalIndependent(s.localIndependent());
        score.setSustainabilityScore(s.sustainabilityScore());
        score.setMaterialFlags(joinFlags(s.materialFlags()));
        score.setConfidence(s.confidence());
        score.setSource(s.source());
        impactScoreRepository.save(score);
    }

    private String displayMerchant(MerchantScoring scoring, TransactionIngested event) {
        if (scoring.cleanedMerchant() != null && !scoring.cleanedMerchant().isBlank()) {
            return scoring.cleanedMerchant();
        }
        return event.merchantName() != null ? event.merchantName() : event.merchantRaw();
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
