-- Ingestion schema: Plaid Items and the raw transactions synced from them.
-- Idempotency is enforced at the DB level via the unique plaid_transaction_id.

CREATE TABLE plaid_item (
    id                   UUID PRIMARY KEY,
    user_id              VARCHAR(64)  NOT NULL,
    plaid_item_id        VARCHAR(128) NOT NULL,
    access_token         VARCHAR(256) NOT NULL,
    institution_name     VARCHAR(256),
    transactions_cursor  TEXT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_plaid_item_id UNIQUE (plaid_item_id)
);

CREATE INDEX idx_plaid_item_user ON plaid_item (user_id);

CREATE TABLE bank_transaction (
    id                     UUID PRIMARY KEY,
    plaid_transaction_id   VARCHAR(128) NOT NULL,
    plaid_item_id          UUID         NOT NULL REFERENCES plaid_item (id),
    user_id                VARCHAR(64)  NOT NULL,
    merchant_raw           VARCHAR(512) NOT NULL,
    merchant_name          VARCHAR(256),
    amount                 NUMERIC(14, 2) NOT NULL,
    iso_currency           VARCHAR(8),
    txn_date               DATE         NOT NULL,
    plaid_category         VARCHAR(256),
    location_city          VARCHAR(128),
    location_region        VARCHAR(128),
    pending                BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Idempotency key: a redelivered webhook / re-sync never duplicates a row.
    CONSTRAINT uq_transaction_plaid_id UNIQUE (plaid_transaction_id)
);

CREATE INDEX idx_transaction_user_date ON bank_transaction (user_id, txn_date);
CREATE INDEX idx_transaction_item ON bank_transaction (plaid_item_id);
