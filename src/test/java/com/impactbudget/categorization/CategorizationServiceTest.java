package com.impactbudget.categorization;

import com.impactbudget.common.TransactionIngested;
import com.impactbudget.common.TransactionScored;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorizationServiceTest {

    @Mock
    MerchantScoreRepository merchantScoreRepository;
    @Mock
    CuratedOverrideService curatedOverrideService;
    @Mock
    MerchantScoringClient scoringClient;
    @Mock
    ImpactScoreRepository impactScoreRepository;
    @Mock
    TransactionScoredPublisher publisher;

    CategorizationService service;

    @BeforeEach
    void setUp() {
        service = new CategorizationService(merchantScoreRepository, curatedOverrideService,
                scoringClient, impactScoreRepository, publisher, new SimpleMeterRegistry());
    }

    private TransactionIngested event() {
        return new TransactionIngested(
                UUID.randomUUID(), "user-1", "TST*SQ*LOCAL COFFEE 12345", "Local Coffee",
                new BigDecimal("4.50"), "USD", LocalDate.of(2026, 7, 1), "Portland", "OR");
    }

    @Test
    void cacheMissCallsClaudeThenCuratedThenPersistsAndPublishes() {
        when(merchantScoreRepository.findByNormalizedMerchant(anyString())).thenReturn(Optional.empty());
        MerchantScoring base = new MerchantScoring("Local Coffee", "Coffee", 85, true, 60,
                List.of(), 0.7, "independent", MerchantScoring.SOURCE_LLM);
        when(scoringClient.score(anyString(), anyString())).thenReturn(base);
        when(curatedOverrideService.apply(anyString(), any())).thenReturn(base);
        when(impactScoreRepository.findByTransactionId(any())).thenReturn(Optional.empty());

        service.categorize(event());

        verify(scoringClient).score(anyString(), anyString());     // LLM was consulted
        verify(merchantScoreRepository).save(any(MerchantScore.class)); // result cached
        verify(impactScoreRepository).save(any(ImpactScore.class));

        ArgumentCaptor<TransactionScored> scored = ArgumentCaptor.forClass(TransactionScored.class);
        verify(publisher).publishScored(scored.capture());
        assertThat(scored.getValue().localScore()).isEqualTo(85);
        assertThat(scored.getValue().amount()).isEqualByComparingTo("4.50");
    }

    @Test
    void cacheHitSkipsClaudeEntirely() {
        MerchantScore cached = new MerchantScore();
        cached.setId(UUID.randomUUID());
        cached.setNormalizedMerchant("LOCAL COFFEE");
        cached.setCleanedMerchant("Local Coffee");
        cached.setLocalScore(85);
        cached.setLocalIndependent(true);
        cached.setSustainabilityScore(60);
        cached.setConfidence(0.7);
        cached.setSource(MerchantScoring.SOURCE_LLM);
        when(merchantScoreRepository.findByNormalizedMerchant(anyString())).thenReturn(Optional.of(cached));
        when(impactScoreRepository.findByTransactionId(any())).thenReturn(Optional.empty());

        service.categorize(event());

        // The whole point of the cache: a repeat merchant never hits the LLM again.
        verify(scoringClient, never()).score(anyString(), anyString());
        verify(merchantScoreRepository, never()).save(any());
        verify(publisher).publishScored(any());
    }
}
