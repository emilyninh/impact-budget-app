package com.impactbudget.ingestion;

import com.plaid.client.model.RemovedTransaction;
import com.plaid.client.model.TransactionsSyncResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Pulls transactions from Plaid using the cursor-based {@code /transactions/sync} endpoint
 * and persists them idempotently.
 *
 * <p>Deliberately not wrapped in a single {@code @Transactional} spanning the whole loop:
 * that would hold a DB connection open across network calls. Instead each row is upserted in
 * its own short transaction ({@link TransactionUpserter}, which also enqueues the outbox
 * event atomically), and idempotency (unique {@code plaid_transaction_id}) makes a re-run
 * after a partial failure safe — the cursor only advances once a page is applied.
 */
@Service
public class TransactionSyncService {

    private static final Logger log = LoggerFactory.getLogger(TransactionSyncService.class);

    private final PlaidGateway plaidGateway;
    private final PlaidItemRepository itemRepository;
    private final BankTransactionRepository txnRepository;
    private final TransactionUpserter upserter;

    public TransactionSyncService(PlaidGateway plaidGateway,
                                  PlaidItemRepository itemRepository,
                                  BankTransactionRepository txnRepository,
                                  TransactionUpserter upserter) {
        this.plaidGateway = plaidGateway;
        this.itemRepository = itemRepository;
        this.txnRepository = txnRepository;
        this.upserter = upserter;
    }

    /** Sync a single Item identified by its Plaid {@code item_id} (as sent in webhooks). */
    public int syncByPlaidItemId(String plaidItemId) {
        PlaidItem item = itemRepository.findByPlaidItemId(plaidItemId)
                .orElseThrow(() -> new PlaidException("Unknown Plaid item_id: " + plaidItemId));
        return sync(item);
    }

    /** Drain all available pages for an Item, advancing and persisting the cursor per page. */
    public int sync(PlaidItem item) {
        int changed = 0;
        String cursor = item.getTransactionsCursor();
        boolean hasMore = true;

        while (hasMore) {
            TransactionsSyncResponse resp = plaidGateway.syncTransactions(item.getAccessToken(), cursor);

            if (resp.getAdded() != null) {
                for (com.plaid.client.model.Transaction t : resp.getAdded()) {
                    upserter.upsert(item, t);
                    changed++;
                }
            }
            if (resp.getModified() != null) {
                for (com.plaid.client.model.Transaction t : resp.getModified()) {
                    upserter.upsert(item, t);
                    changed++;
                }
            }
            if (resp.getRemoved() != null) {
                for (RemovedTransaction r : resp.getRemoved()) {
                    txnRepository.deleteByPlaidTransactionId(r.getTransactionId());
                    changed++;
                }
            }

            cursor = resp.getNextCursor();
            hasMore = Boolean.TRUE.equals(resp.getHasMore());

            // Persist cursor progress so a resync resumes from here, not from scratch.
            item.setTransactionsCursor(cursor);
            itemRepository.save(item);
        }

        log.info("Synced Plaid item {} — {} transaction changes applied", item.getPlaidItemId(), changed);
        return changed;
    }
}
