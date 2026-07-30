-- Carry the per-transaction scoring confidence onto the budget projection so monthly aggregates
-- can be confidence-weighted (a guessed 50 shouldn't count the same as a grounded 90) and report
-- how much of the spend is actually scored vs unknown. Defaults to the neutral-fallback confidence.

ALTER TABLE scored_transaction
    ADD COLUMN confidence DOUBLE PRECISION NOT NULL DEFAULT 0.2;
