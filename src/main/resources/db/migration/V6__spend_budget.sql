-- A single overall monthly spending limit per user (e.g. $3,000/month). Spend is tracked
-- against this limit using the existing monthly totalSpend aggregate. One active row per user.
CREATE TABLE spend_budget (
    id            UUID PRIMARY KEY,
    user_id       VARCHAR(64)    NOT NULL UNIQUE,
    monthly_limit NUMERIC(12, 2) NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);
