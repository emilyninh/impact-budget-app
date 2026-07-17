-- Application users for JWT auth + per-user multi-tenancy. The user's UUID (as text) is the
-- userId carried on every domain row (bank_transaction, scored_transaction, goal, …), so data
-- isolation is enforced by the authenticated principal rather than a client-supplied param.
CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(120),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
