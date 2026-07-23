package com.impactbudget.categorization;

import com.impactbudget.common.TransactionIngested;
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
    OpenFoodFactsEnricher openFoodFactsEnricher;
    @Mock
    WikidataLocalEnricher wikidataLocalEnricher;
    @Mock
    ScoringPersistence scoringPersistence;

    CategorizationService service;

    @BeforeEach
    void setUp() {
        service = new CategorizationService(merchantScoreRepository, curatedOverrideService,
                scoringClient, openFoodFactsEnricher, wikidataLocalEnricher,
                new MerchantCategoryResolver(), scoringPersistence, new SimpleMeterRegistry());
    }

    private TransactionIngested event() {
        return new TransactionIngested(
                UUID.randomUUID(), "user-1", "TST*SQ*LOCAL COFFEE 12345", "Local Coffee",
                new BigDecimal("4.50"), "USD", LocalDate.of(2026, 7, 1), "Portland", "OR", null);
    }

    @Test
    void cacheMissCallsScorerThenCuratedThenCachesAndPersists() {
        when(merchantScoreRepository.findByNormalizedMerchant(anyString())).thenReturn(Optional.empty());
        MerchantScoring base = new MerchantScoring("Local Coffee", "Coffee", 85, true, 60,
                List.of(), 0.7, "independent", MerchantScoring.SOURCE_LLM);
        when(scoringClient.score(anyString(), anyString())).thenReturn(base);
        // Enrichers pass the scoring through unchanged for this test.
        when(openFoodFactsEnricher.enrich(anyString(), any())).thenAnswer(inv -> inv.getArgument(1));
        when(wikidataLocalEnricher.enrich(anyString(), any())).thenAnswer(inv -> inv.getArgument(1));
        when(curatedOverrideService.apply(anyString(), any())).thenReturn(base);

        service.categorize(event());

        verify(scoringClient).score(anyString(), anyString());          // scorer was consulted
        verify(merchantScoreRepository).save(any(MerchantScore.class));  // result cached

        // Persistence + outbox enqueue happen atomically in the collaborator.
        ArgumentCaptor<TransactionIngested> evt = ArgumentCaptor.forClass(TransactionIngested.class);
        ArgumentCaptor<MerchantScoring> scoring = ArgumentCaptor.forClass(MerchantScoring.class);
        verify(scoringPersistence).persist(evt.capture(), scoring.capture());
        assertThat(scoring.getValue().localScore()).isEqualTo(85);
        assertThat(evt.getValue().amount()).isEqualByComparingTo("4.50");
        // Free-text LLM category "Coffee" is normalized onto the fixed taxonomy.
        assertThat(scoring.getValue().category()).isEqualTo("Eating Out");
    }

    @Test
    void cacheHitSkipsScorerEntirely() {
        MerchantScore cached = new MerchantScore();
        cached.setId(UUID.randomUUID());
        cached.setNormalizedMerchant("LOCAL COFFEE");
        cached.setCleanedMerchant("Local Coffee");
        cached.setCategory("Eating Out");
        cached.setLocalScore(85);
        cached.setLocalIndependent(true);
        cached.setSustainabilityScore(60);
        cached.setConfidence(0.7);
        cached.setSource(MerchantScoring.SOURCE_LLM);
        when(merchantScoreRepository.findByNormalizedMerchant(anyString())).thenReturn(Optional.of(cached));

        service.categorize(event());

        // The whole point of the cache: a repeat merchant never hits the scorer again.
        verify(scoringClient, never()).score(anyString(), anyString());
        verify(merchantScoreRepository, never()).save(any());
        verify(scoringPersistence).persist(any(), any());
    }
}
