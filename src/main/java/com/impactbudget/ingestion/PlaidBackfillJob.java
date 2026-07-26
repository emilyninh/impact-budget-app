package com.impactbudget.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Auto-backfills freshly-linked Plaid items. After linking, Plaid readies transactions
 * asynchronously, so the initial sync often returns nothing. Rather than making the user click
 * "sync", this job re-syncs any item with an open backfill window until transactions arrive
 * (then closes the window) or the window expires (then gives up). Combined with SSE, the
 * dashboard fills in on its own after connecting a bank — no webhook needed locally.
 */
@Component
class PlaidBackfillJob {

    private static final Logger log = LoggerFactory.getLogger(PlaidBackfillJob.class);

    private final PlaidItemRepository itemRepository;
    private final TransactionSyncService syncService;

    PlaidBackfillJob(PlaidItemRepository itemRepository, TransactionSyncService syncService) {
        this.itemRepository = itemRepository;
        this.syncService = syncService;
    }

    @Scheduled(fixedDelayString = "${plaid.backfill.interval-ms:7000}")
    void backfillPendingItems() {
        Instant now = Instant.now();
        for (PlaidItem item : itemRepository.findByBackfillUntilIsNotNull()) {
            if (now.isAfter(item.getBackfillUntil())) {
                item.setBackfillUntil(null);   // window elapsed — stop trying
                itemRepository.save(item);
                continue;
            }
            try {
                int changed = syncService.sync(item);
                if (changed > 0) {
                    item.setBackfillUntil(null);   // data arrived — done
                    itemRepository.save(item);
                    log.info("Backfill complete for Plaid item {} ({} changes)",
                            item.getPlaidItemId(), changed);
                }
            } catch (Exception e) {
                log.warn("Backfill sync failed for Plaid item {} ({})", item.getPlaidItemId(), e.toString());
            }
        }
    }
}
