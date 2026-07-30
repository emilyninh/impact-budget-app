package com.impactbudget.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Handles the Plaid Link handshake: minting a Link token for the frontend and exchanging
 * the resulting public token for a persistent access token, which is stored as a
 * {@link PlaidItem}. Immediately triggers an initial transaction sync so data is available
 * right after linking.
 */
@Service
public class PlaidLinkService {

    private static final Logger log = LoggerFactory.getLogger(PlaidLinkService.class);
    /** How long after linking to keep auto-retrying the sync while Plaid readies transactions. */
    private static final Duration BACKFILL_WINDOW = Duration.ofMinutes(3);

    private final PlaidGateway plaidGateway;
    private final PlaidItemRepository itemRepository;
    private final TransactionSyncService syncService;

    public PlaidLinkService(PlaidGateway plaidGateway,
                            PlaidItemRepository itemRepository,
                            TransactionSyncService syncService) {
        this.plaidGateway = plaidGateway;
        this.itemRepository = itemRepository;
        this.syncService = syncService;
    }

    public String createLinkToken(String userId) {
        return plaidGateway.createLinkToken(userId);
    }

    /**
     * Exchange a Link public token for an access token, persist the Item, and kick off an
     * initial sync. Returns the Plaid {@code item_id}.
     */
    public String exchangePublicToken(String publicToken, String userId) {
        PlaidGateway.ExchangeResult result = plaidGateway.exchangePublicToken(publicToken);

        PlaidItem item = itemRepository.findByPlaidItemId(result.itemId())
                .orElseGet(PlaidItem::new);
        if (item.getId() == null) {
            item.setId(UUID.randomUUID());
            item.setPlaidItemId(result.itemId());
        }
        item.setUserId(userId);
        item.setAccessToken(result.accessToken());
        // Capture the institution name (e.g. "Chase") so transactions can be labeled by account.
        if (!org.springframework.util.StringUtils.hasText(item.getInstitutionName())) {
            item.setInstitutionName(plaidGateway.fetchInstitutionName(result.accessToken()));
        }
        // Open a backfill window: Plaid readies transactions asynchronously, so if the first
        // sync comes back empty the scheduled PlaidBackfillJob keeps retrying until they arrive.
        item.setBackfillUntil(Instant.now().plus(BACKFILL_WINDOW));
        item = itemRepository.save(item);

        log.info("Linked Plaid item {} for user {}", item.getPlaidItemId(), userId);

        // Try immediately; if data's already there, close the backfill window.
        int changed = syncService.sync(item);
        if (changed > 0) {
            item.setBackfillUntil(null);
            itemRepository.save(item);
        }
        return item.getPlaidItemId();
    }

    /**
     * Re-sync all of a user's linked Plaid items on demand — useful when sandbox transactions
     * weren't ready at link time, or to refresh without a webhook. Returns the number of changes.
     * Skips synthetic items (CSV import, demo seed) that aren't backed by a Plaid access token.
     */
    public int syncAll(String userId) {
        int changed = 0;
        for (PlaidItem item : itemRepository.findByUserId(userId)) {
            String token = item.getAccessToken();
            if (token != null && token.startsWith("access-")) {   // real Plaid item only
                changed += syncService.sync(item);
            }
        }
        return changed;
    }
}
