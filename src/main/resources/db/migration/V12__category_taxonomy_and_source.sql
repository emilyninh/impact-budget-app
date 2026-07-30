-- Richer categorization + source metadata.
--
-- Capture Plaid's detailed personal-finance category (the primary was already stored in
-- plaid_category) so the taxonomy mapper can split e.g. Groceries vs Eating Out. And carry the
-- source institution + a transfer/non-spend flag onto the budget-owned projection so the ledger
-- can show which bank a transaction came from and exclude account-to-account transfers from spend.

ALTER TABLE bank_transaction
    ADD COLUMN plaid_category_detailed VARCHAR(128);

ALTER TABLE scored_transaction
    ADD COLUMN institution_name VARCHAR(128);

ALTER TABLE scored_transaction
    ADD COLUMN excluded_from_spend BOOLEAN NOT NULL DEFAULT FALSE;
