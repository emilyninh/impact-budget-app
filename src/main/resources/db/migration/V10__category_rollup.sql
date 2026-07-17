-- Persisted per-category monthly rollups, maintained incrementally as scored transactions
-- arrive (safe from double-counting because the budget consumer is idempotent). Powers the
-- category-breakdown chart without scanning scored_transaction on every request.
CREATE TABLE category_monthly_rollup (
    user_id                 VARCHAR(64)    NOT NULL,
    year_month              VARCHAR(7)     NOT NULL,
    category                VARCHAR(64)    NOT NULL,
    total_spend             NUMERIC(14, 2) NOT NULL DEFAULT 0,
    txn_count               INT            NOT NULL DEFAULT 0,
    sustainability_weighted NUMERIC(18, 2) NOT NULL DEFAULT 0,   -- sum(amount * sustainability_score)
    PRIMARY KEY (user_id, year_month, category)
);
