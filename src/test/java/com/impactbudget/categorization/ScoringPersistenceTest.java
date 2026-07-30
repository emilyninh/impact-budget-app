package com.impactbudget.categorization;

import com.impactbudget.common.TransactionIngested;
import com.impactbudget.common.TransactionScored;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoringPersistenceTest {

    @Mock
    ImpactScoreRepository impactScoreRepository;
    @Mock
    TransactionScoredPublisher publisher;

    private TransactionIngested event(UUID txnId) {
        return new TransactionIngested(txnId, "user-1", "TST*LOCAL COFFEE", "Local Coffee",
                new BigDecimal("4.50"), "USD", LocalDate.of(2026, 7, 1), "Portland", "OR",
                null, null, "Chase");
    }

    @Test
    void savesImpactScoreAndPublishesScoredEvent() {
        ScoringPersistence persistence = new ScoringPersistence(impactScoreRepository, publisher);
        UUID txnId = UUID.randomUUID();
        when(impactScoreRepository.findByTransactionId(txnId)).thenReturn(Optional.empty());
        MerchantScoring scoring = new MerchantScoring("Local Coffee", "Eating Out", 85, true, 60,
                List.of("organic"), 0.7, "independent", MerchantScoring.SOURCE_LLM);

        persistence.persist(event(txnId), scoring, "Eating Out");

        ArgumentCaptor<ImpactScore> score = ArgumentCaptor.forClass(ImpactScore.class);
        verify(impactScoreRepository).save(score.capture());
        assertThat(score.getValue().getTransactionId()).isEqualTo(txnId);
        assertThat(score.getValue().getCategory()).isEqualTo("Eating Out");
        assertThat(score.getValue().getLocalScore()).isEqualTo(85);

        ArgumentCaptor<TransactionScored> event = ArgumentCaptor.forClass(TransactionScored.class);
        verify(publisher).publishScored(event.capture());
        assertThat(event.getValue().transactionId()).isEqualTo(txnId);
        assertThat(event.getValue().merchantName()).isEqualTo("Local Coffee");
        assertThat(event.getValue().category()).isEqualTo("Eating Out");
        assertThat(event.getValue().amount()).isEqualByComparingTo("4.50");
        assertThat(event.getValue().institutionName()).isEqualTo("Chase");
    }

    @Test
    void reusesExistingImpactScoreRowForIdempotency() {
        ScoringPersistence persistence = new ScoringPersistence(impactScoreRepository, publisher);
        UUID txnId = UUID.randomUUID();
        ImpactScore existing = new ImpactScore();
        existing.setId(UUID.randomUUID());
        existing.setTransactionId(txnId);
        when(impactScoreRepository.findByTransactionId(txnId)).thenReturn(Optional.of(existing));
        MerchantScoring scoring = new MerchantScoring("Local Coffee", "Eating Out", 85, true, 60,
                List.of(), 0.7, "independent", MerchantScoring.SOURCE_LLM);

        persistence.persist(event(txnId), scoring, "Eating Out");

        ArgumentCaptor<ImpactScore> score = ArgumentCaptor.forClass(ImpactScore.class);
        verify(impactScoreRepository).save(score.capture());
        // Same row updated, not a new id.
        assertThat(score.getValue().getId()).isEqualTo(existing.getId());
        verify(publisher).publishScored(any());
    }
}
