-- When a bank is linked, Plaid generates transactions asynchronously — the first sync often
-- returns nothing. This column marks an item for auto-backfill: a scheduled job keeps
-- re-syncing until transactions arrive or this deadline passes, so the user never has to click
-- "sync" after linking.
ALTER TABLE plaid_item ADD COLUMN backfill_until TIMESTAMPTZ;
