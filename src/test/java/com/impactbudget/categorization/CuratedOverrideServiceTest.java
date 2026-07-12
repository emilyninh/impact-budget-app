package com.impactbudget.categorization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuratedOverrideServiceTest {

    @Mock
    CuratedMerchantRepository repository;

    @InjectMocks
    CuratedOverrideService service;

    private CuratedMerchant starbucks() {
        CuratedMerchant c = new CuratedMerchant();
        c.setMatchKey("STARBUCKS");
        c.setDisplayName("Starbucks");
        c.setLocalScore(5);
        c.setLocalIndependent(false);
        c.setSustainabilityScore(45);
        c.setNote("Multinational coffee chain");
        return c;
    }

    private MerchantScoring llmGuess() {
        // Pretend the LLM guessed high local/sustainability — curated data must correct it.
        return new MerchantScoring("Starbucks", "Coffee", 80, true, 70,
                List.of(), 0.6, "guess", MerchantScoring.SOURCE_LLM);
    }

    @Test
    void curatedGroundTruthOverridesLlmGuess() {
        when(repository.findAll()).thenReturn(List.of(starbucks()));

        MerchantScoring result = service.apply("STARBUCKS COFFEE", llmGuess());

        assertThat(result.source()).isEqualTo(MerchantScoring.SOURCE_CURATED);
        assertThat(result.localScore()).isEqualTo(5);
        assertThat(result.localIndependent()).isFalse();
        assertThat(result.sustainabilityScore()).isEqualTo(45);
        assertThat(result.cleanedMerchant()).isEqualTo("Starbucks");
        assertThat(result.confidence()).isGreaterThan(0.9);
    }

    @Test
    void unmatchedMerchantPassesBaseThrough() {
        when(repository.findAll()).thenReturn(List.of(starbucks()));

        MerchantScoring base = new MerchantScoring("Rosie's Cafe", "Coffee", 90, true, 65,
                List.of(), 0.5, "local cafe", MerchantScoring.SOURCE_LLM);

        MerchantScoring result = service.apply("ROSIES CAFE", base);

        assertThat(result).isEqualTo(base);
    }
}
