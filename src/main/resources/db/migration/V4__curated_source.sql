-- Provenance for curated entries: MANUAL (hand-seeded in V2), B-CORP (loaded from the
-- B Corp dataset by CuratedMerchantSeeder), etc.
ALTER TABLE curated_merchant ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'MANUAL';
