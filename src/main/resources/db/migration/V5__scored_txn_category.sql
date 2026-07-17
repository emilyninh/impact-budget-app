-- Surface the spending category (Eating Out, Groceries, Shopping, …) on the budget-owned
-- projection so the dashboard transaction list can show it. Category already flows through
-- the TransactionScored event; this is the last mile into the budget module.
ALTER TABLE scored_transaction ADD COLUMN category VARCHAR(64);
