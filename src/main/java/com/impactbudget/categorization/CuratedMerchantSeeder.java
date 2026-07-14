package com.impactbudget.categorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Loads Certified B Corporations from {@code classpath:data/bcorp-seed.csv} into
 * {@code curated_merchant} on startup. Idempotent (skips existing match keys), so it
 * coexists with the V2 hand-seed and is safe on every boot. Replace the CSV with the full
 * B Lab directory export (same columns) to widen coverage.
 *
 * <p>B Corp rows set only the sustainability dimension and material flags; the local score
 * is left to Wikidata/LLM (a B Corp can be owned by a multinational).
 */
@Component
class CuratedMerchantSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CuratedMerchantSeeder.class);
    private static final String RESOURCE = "data/bcorp-seed.csv";

    private final CuratedMerchantRepository repository;

    CuratedMerchantSeeder(CuratedMerchantRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(RESOURCE).getInputStream(), StandardCharsets.UTF_8))) {

            int inserted = 0;
            for (CuratedRow row : parse(reader)) {
                if (repository.existsByMatchKey(row.matchKey())) {
                    continue;   // idempotent: don't duplicate V2 seed or prior runs
                }
                CuratedMerchant c = new CuratedMerchant();
                c.setId(UUID.randomUUID());
                c.setMatchKey(row.matchKey());
                c.setDisplayName(row.displayName());
                c.setSustainabilityScore(row.sustainabilityScore());
                c.setMaterialFlags(row.materialFlags());   // may be null
                c.setNote(row.note());
                c.setSource("B-CORP");
                // localScore / localIndependent left null — B Corp says nothing about "local".
                repository.save(c);
                inserted++;
            }
            log.info("B Corp seed: {} new curated merchants loaded", inserted);
        } catch (Exception e) {
            // Seeding is best-effort; never fail startup over it.
            log.warn("B Corp seed skipped ({})", e.toString());
        }
    }

    /** Parse the pipe-delimited seed file, ignoring blank and {@code #} comment lines. */
    static List<CuratedRow> parse(BufferedReader reader) throws java.io.IOException {
        List<CuratedRow> rows = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] parts = trimmed.split("\\|", -1);
            if (parts.length < 3) {
                continue;
            }
            String matchKey = parts[0].trim().toUpperCase();
            String displayName = parts[1].trim();
            int sustainability = parseIntSafe(parts[2].trim());
            String materialFlags = parts.length > 3 && !parts[3].isBlank() ? parts[3].trim() : null;
            String note = parts.length > 4 && !parts[4].isBlank() ? parts[4].trim() : null;
            if (!matchKey.isEmpty() && !displayName.isEmpty()) {
                rows.add(new CuratedRow(matchKey, displayName, sustainability, materialFlags, note));
            }
        }
        return rows;
    }

    private static int parseIntSafe(String s) {
        try {
            return Math.max(0, Math.min(100, Integer.parseInt(s)));
        } catch (NumberFormatException e) {
            return 70;   // sensible default for a certified B Corp
        }
    }

    record CuratedRow(String matchKey, String displayName, int sustainabilityScore,
                      String materialFlags, String note) {
    }
}
