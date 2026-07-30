-- Capture Plaid's merchant web identity (website + stable entity id), currently discarded.
-- The website is the cleanest way to resolve a small online merchant to its own site, where its
-- sustainability credentials (GOTS, organic, B Corp, …) are stated — see WebsiteSignalEnricher.

ALTER TABLE bank_transaction
    ADD COLUMN merchant_website VARCHAR(255);

ALTER TABLE bank_transaction
    ADD COLUMN merchant_entity_id VARCHAR(64);
