-- Curated ground truth for local, independent food businesses used in the demo (and real data).
-- A farmers market / neighborhood food co-op genuinely is local & independent, so this is legitimate
-- ground truth, not demo-gaming. Curated scores win on the chain, so these are reliable without a
-- network call. (The unknown sustainable brands SimpleEcology / Lina Lennox are deliberately NOT
-- curated — the website-signal enricher discovers them.)

INSERT INTO curated_merchant
    (match_key, display_name, local_score, local_independent, sustainability_score, material_flags, note)
VALUES
    ('FARMERS MARKET', 'Farmers Market',       95, TRUE, 82, 'local,seasonal',
        'Local farmers market — independent growers'),
    ('ALBERTA COOP',   'Alberta Co-op Grocery', 90, TRUE, 75, 'organic,local,cooperative',
        'Independent neighborhood food co-op');
