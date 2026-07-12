-- Categorization schema: the merchant-score cache, the curated ground-truth overrides,
-- and the per-transaction impact scores.

-- Cache of scores per normalized merchant. A hit here means no LLM call is needed.
CREATE TABLE merchant_score (
    id                    UUID PRIMARY KEY,
    normalized_merchant   VARCHAR(256) NOT NULL,
    cleaned_merchant      VARCHAR(256),
    category              VARCHAR(128),
    local_score           INT          NOT NULL,
    local_independent     BOOLEAN      NOT NULL,
    sustainability_score  INT          NOT NULL,
    material_flags        TEXT,                       -- comma-separated
    confidence            DOUBLE PRECISION NOT NULL,
    rationale             TEXT,
    source                VARCHAR(16)  NOT NULL,      -- LLM | FALLBACK | CURATED
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_merchant_score_key UNIQUE (normalized_merchant)
);

-- Curated ground truth. Corrects the LLM: a normalized merchant containing a match_key
-- gets these fields overlaid (curated wins on conflict). Nullable columns override only
-- the dimensions they specify.
CREATE TABLE curated_merchant (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_key             VARCHAR(128) NOT NULL,
    display_name          VARCHAR(128) NOT NULL,
    local_score           INT,
    local_independent     BOOLEAN,
    sustainability_score  INT,
    material_flags        TEXT,
    note                  TEXT,
    CONSTRAINT uq_curated_match_key UNIQUE (match_key)
);

-- Per-transaction impact scores (the resolved result after cache/LLM/overrides).
CREATE TABLE impact_score (
    id                    UUID PRIMARY KEY,
    transaction_id        UUID         NOT NULL REFERENCES bank_transaction (id),
    user_id               VARCHAR(64)  NOT NULL,
    category              VARCHAR(128),
    local_score           INT          NOT NULL,
    local_independent     BOOLEAN      NOT NULL,
    sustainability_score  INT          NOT NULL,
    material_flags        TEXT,
    confidence            DOUBLE PRECISION NOT NULL,
    source                VARCHAR(16)  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_impact_score_txn UNIQUE (transaction_id)
);

CREATE INDEX idx_impact_score_user ON impact_score (user_id);

-- Seed curated ground truth. Local scores: 0 = pure multinational, 100 = local/independent.
-- Sustainability: 0 = poor (fast fashion / high-footprint), 100 = strong (B-Corp / organic).
INSERT INTO curated_merchant (match_key, display_name, local_score, local_independent, sustainability_score, material_flags, note) VALUES
    ('STARBUCKS',  'Starbucks',        5,   FALSE, 45,  NULL,                      'Multinational coffee chain'),
    ('MCDONALDS',  'McDonald''s',      3,   FALSE, 25,  NULL,                      'Multinational fast food'),
    ('WALMART',    'Walmart',          2,   FALSE, 30,  NULL,                      'Multinational big-box retailer'),
    ('AMAZON',     'Amazon',           2,   FALSE, 35,  NULL,                      'Multinational e-commerce'),
    ('TARGET',     'Target',           5,   FALSE, 40,  NULL,                      'National big-box retailer'),
    ('WHOLEFOODS', 'Whole Foods',      8,   FALSE, 60,  'organic',                 'Amazon-owned, organic focus'),
    ('SHEIN',      'Shein',            1,   FALSE, 5,   'polyester,fast-fashion',  'Ultra-fast fashion, synthetic'),
    ('H&M',        'H&M',              4,   FALSE, 20,  'polyester,fast-fashion',  'Fast fashion, mostly synthetic'),
    ('ZARA',       'Zara',             4,   FALSE, 22,  'fast-fashion',            'Fast fashion'),
    ('PATAGONIA',  'Patagonia',        10,  FALSE, 95,  'organic-cotton,recycled', 'B-Corp, sustainable outdoor brand'),
    ('REI',        'REI',              15,  FALSE, 85,  'recycled',                'Co-op, sustainability focus'),
    ('ALLBIRDS',   'Allbirds',         8,   FALSE, 88,  'merino-wool,natural',     'B-Corp, natural fibers');
