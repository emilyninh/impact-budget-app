-- Budget schema. The budget module owns its own projection of scored transactions
-- (fed by TransactionScored events) so monthly aggregates can be rebuilt from
-- budget-owned data without reaching into other modules' tables. Redis caches the
-- computed aggregates and is invalidated whenever a new scored transaction lands.

CREATE TABLE scored_transaction (
    id                    UUID PRIMARY KEY,
    transaction_id        UUID         NOT NULL,
    user_id               VARCHAR(64)  NOT NULL,
    merchant_name         VARCHAR(256),
    year_month            VARCHAR(7)   NOT NULL,      -- e.g. 2026-07
    txn_date              DATE         NOT NULL,
    amount                NUMERIC(14, 2) NOT NULL,
    local_score           INT          NOT NULL,
    sustainability_score  INT          NOT NULL,
    local_independent     BOOLEAN      NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_scored_txn UNIQUE (transaction_id)   -- idempotent on re-delivery
);

CREATE INDEX idx_scored_txn_user_month ON scored_transaction (user_id, year_month);

-- A user's goal to shift a dimension of discretionary spending over time.
CREATE TABLE goal (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       VARCHAR(64) NOT NULL,
    dimension     VARCHAR(16) NOT NULL,   -- LOCAL | SUSTAINABLE
    baseline_pct  INT         NOT NULL,   -- where they started
    target_pct    INT         NOT NULL,   -- where they want to be
    target_date   DATE        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_goal_user ON goal (user_id);
