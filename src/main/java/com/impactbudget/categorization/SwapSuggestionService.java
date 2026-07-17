package com.impactbudget.categorization;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Suggests higher-sustainability merchants in the same spending category, drawn from the
 * merchant-score cache. The public API the dashboard's recommendation feature builds on.
 */
@Service
public class SwapSuggestionService {

    private final MerchantScoreRepository repository;

    public SwapSuggestionService(MerchantScoreRepository repository) {
        this.repository = repository;
    }

    /**
     * Greener alternatives in {@code category} scoring at least {@code minSustainability},
     * excluding the merchant being swapped, capped at {@code limit}.
     */
    public List<GreenerAlternative> greenerAlternatives(String category, int minSustainability,
                                                        String excludeMerchant, int limit) {
        if (category == null) {
            return List.of();
        }
        return repository
                .findTop10ByCategoryAndSustainabilityScoreGreaterThanEqualOrderBySustainabilityScoreDesc(
                        category, minSustainability)
                .stream()
                .filter(m -> !sameMerchant(m, excludeMerchant))
                .limit(limit)
                .map(m -> new GreenerAlternative(displayName(m), m.getSustainabilityScore(),
                        splitFlags(m.getMaterialFlags())))
                .toList();
    }

    private boolean sameMerchant(MerchantScore m, String exclude) {
        if (exclude == null) {
            return false;
        }
        String name = displayName(m);
        return name != null && name.equalsIgnoreCase(exclude);
    }

    private static String displayName(MerchantScore m) {
        return m.getCleanedMerchant() != null ? m.getCleanedMerchant() : m.getNormalizedMerchant();
    }

    private static List<String> splitFlags(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }
}
