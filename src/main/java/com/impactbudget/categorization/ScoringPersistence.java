package com.impactbudget.categorization;

import com.impactbudget.common.TransactionIngested;
import com.impactbudget.common.TransactionScored;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Writes the per-transaction {@link ImpactScore} and enqueues the {@link TransactionScored}
 * event in one transaction (transactional outbox). Separate bean so {@code @Transactional}
 * takes effect and the network-bound scoring in {@link CategorizationService} stays outside
 * the transaction.
 */
@Component
class ScoringPersistence {

    private final ImpactScoreRepository impactScoreRepository;
    private final TransactionScoredPublisher publisher;

    ScoringPersistence(ImpactScoreRepository impactScoreRepository, TransactionScoredPublisher publisher) {
        this.impactScoreRepository = impactScoreRepository;
        this.publisher = publisher;
    }

    @Transactional
    void persist(TransactionIngested event, MerchantScoring scoring, String category) {
        saveImpactScore(event, scoring, category);
        publisher.publishScored(new TransactionScored(
                event.transactionId(), event.userId(), displayMerchant(scoring, event), event.amount(),
                event.txnDate(), category, scoring.localScore(), scoring.localIndependent(),
                scoring.sustainabilityScore(), scoring.materialFlags(),
                scoring.confidence(), scoring.source(), event.institutionName()));
    }

    private void saveImpactScore(TransactionIngested event, MerchantScoring s, String category) {
        ImpactScore score = impactScoreRepository.findByTransactionId(event.transactionId())
                .orElseGet(ImpactScore::new);
        if (score.getId() == null) {
            score.setId(UUID.randomUUID());
            score.setTransactionId(event.transactionId());
        }
        score.setUserId(event.userId());
        score.setCategory(category);
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

    private static String joinFlags(List<String> flags) {
        return (flags == null || flags.isEmpty()) ? null : String.join(",", flags);
    }
}
