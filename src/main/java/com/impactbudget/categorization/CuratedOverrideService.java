package com.impactbudget.categorization;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Overlays curated ground truth on top of an LLM/fallback assessment. The curated table
 * (seeded national chains, B-Corps, fast-fashion brands) is authoritative: any dimension
 * it specifies wins over the model's guess.
 */
@Service
public class CuratedOverrideService {

    private final CuratedMerchantRepository repository;

    public CuratedOverrideService(CuratedMerchantRepository repository) {
        this.repository = repository;
    }

    /**
     * Apply the first curated entry whose {@code matchKey} is a substring of the normalized
     * merchant. Returns the base scoring unchanged when nothing matches.
     */
    public MerchantScoring apply(String normalizedMerchant, MerchantScoring base) {
        CuratedMerchant match = findMatch(normalizedMerchant);
        if (match == null) {
            return base;
        }

        int localScore = match.getLocalScore() != null ? match.getLocalScore() : base.localScore();
        boolean localIndependent = match.getLocalIndependent() != null
                ? match.getLocalIndependent() : base.localIndependent();
        int sustainabilityScore = match.getSustainabilityScore() != null
                ? match.getSustainabilityScore() : base.sustainabilityScore();
        List<String> materialFlags = match.getMaterialFlags() != null
                ? splitFlags(match.getMaterialFlags()) : base.materialFlags();
        String category = base.category() != null ? base.category() : null;

        String rationale = "Curated override: " + match.getDisplayName()
                + (match.getNote() != null ? " — " + match.getNote() : "");

        return new MerchantScoring(
                match.getDisplayName(),
                category,
                localScore,
                localIndependent,
                sustainabilityScore,
                materialFlags,
                0.99,                       // curated ground truth is high-confidence
                rationale,
                MerchantScoring.SOURCE_CURATED);
    }

    private CuratedMerchant findMatch(String normalizedMerchant) {
        if (normalizedMerchant == null || normalizedMerchant.isBlank()) {
            return null;
        }
        String haystack = normalizedMerchant.toUpperCase();
        return repository.findAll().stream()
                .filter(c -> haystack.contains(c.getMatchKey().toUpperCase()))
                .findFirst()
                .orElse(null);
    }

    private List<String> splitFlags(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
