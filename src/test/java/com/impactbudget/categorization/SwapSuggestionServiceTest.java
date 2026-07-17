package com.impactbudget.categorization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SwapSuggestionServiceTest {

    @Mock
    MerchantScoreRepository repository;

    private MerchantScore merchant(String name, String category, int sustainability) {
        MerchantScore m = new MerchantScore();
        m.setNormalizedMerchant(name.toUpperCase());
        m.setCleanedMerchant(name);
        m.setCategory(category);
        m.setSustainabilityScore(sustainability);
        return m;
    }

    @Test
    void returnsHigherScoringAlternativesExcludingSelfAndRespectingLimit() {
        when(repository.findTop10ByCategoryAndSustainabilityScoreGreaterThanEqualOrderBySustainabilityScoreDesc(
                "Shopping", 30))
                .thenReturn(List.of(
                        merchant("Patagonia", "Shopping", 95),
                        merchant("Allbirds", "Shopping", 88),
                        merchant("Shein", "Shopping", 40)));   // the merchant being replaced

        SwapSuggestionService service = new SwapSuggestionService(repository);
        List<GreenerAlternative> out = service.greenerAlternatives("Shopping", 30, "Shein", 2);

        assertThat(out).hasSize(2);
        assertThat(out).extracting(GreenerAlternative::merchant).containsExactly("Patagonia", "Allbirds");
        assertThat(out.get(0).sustainabilityScore()).isEqualTo(95);
    }

    @Test
    void nullCategoryYieldsNoSuggestions() {
        SwapSuggestionService service = new SwapSuggestionService(repository);
        assertThat(service.greenerAlternatives(null, 30, "Shein", 3)).isEmpty();
    }
}
