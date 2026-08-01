-- Curated ground truth for the demo: three sustainable brands (OSEA, Avocado Mattress, Thrive
-- Market) plus two known merchants whose raw bank descriptors were leaking into the ledger
-- ("TRADER JOES 210", "TST*ROSIES CORNER CAFE"). A curated row is the final authority on the
-- scoring chain (CuratedOverrideService, confidence 0.99) and sets the DISPLAYED merchant name to
-- display_name — so this both scores these merchants reliably (offline, no network) AND fixes the
-- ledger label in one step.
--
-- match_key is matched as: normalize(descriptor).toUpperCase().contains(match_key) — first hit wins.
-- MerchantNormalizer drops digit tokens and processor noise (TST/SP/WWW/…), so e.g.
--   "TRADER JOES 210"        -> "TRADER JOES"
--   "TST*ROSIES CORNER CAFE" -> "ROSIES CORNER CAFE"
-- Each match_key below is a contiguous substring of the normalized descriptor.
--
-- These are legitimate ground truth (real brands with real credentials), consistent with the
-- existing B-Corp curation. Lina Lennox / SimpleEcology stay deliberately UNCURATED so the
-- website-signal enricher still discovers them live in the demo.

-- Upsert (not plain INSERT): 'THRIVE MARKET' is already seeded from bcorp-seed.csv, so a plain
-- insert would violate uq_curated_match_key. DO UPDATE also makes this migration authoritative
-- (these curated values win) and safe to re-run.
INSERT INTO curated_merchant
    (match_key, display_name, local_score, local_independent, sustainability_score, material_flags, note)
VALUES
    ('TRADER JOES',      'Trader Joe''s',        15, FALSE, 50, 'private-label',
        'National grocery chain'),
    ('ROSIES CORNER',    'Rosie''s Corner Cafe', 90, TRUE,  55, 'local,independent',
        'Independent neighborhood cafe'),
    ('OSEA MALIBU',      'OSEA',                 20, FALSE, 82, 'vegan,cruelty-free,clean-beauty,climate-neutral',
        'Clean-beauty skincare, climate neutral'),
    ('AVOCADO MATTRESS', 'Avocado Mattress',     25, FALSE, 92, 'gots-organic,gols-organic,b-corp,1%-for-planet',
        'Certified-organic mattresses, B Corp'),
    ('THRIVE MARKET',    'Thrive Market',        15, FALSE, 80, 'organic,b-corp,carbon-neutral',
        'Organic online grocery, B Corp')
ON CONFLICT (match_key) DO UPDATE SET
    display_name         = EXCLUDED.display_name,
    local_score          = EXCLUDED.local_score,
    local_independent    = EXCLUDED.local_independent,
    sustainability_score = EXCLUDED.sustainability_score,
    material_flags       = EXCLUDED.material_flags,
    note                 = EXCLUDED.note;
