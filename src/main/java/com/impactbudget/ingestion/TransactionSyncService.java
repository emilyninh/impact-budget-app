package com.impactbudget.ingestion;

import com.plaid.client.model.Location;
import com.plaid.client.model.PersonalFinanceCategory;
import com.plaid.client.model.RemovedTransaction;
import com.plaid.client.model.TransactionsSyncResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Pulls transactions from Plaid using the cursor-based {@code /transactions/sync} endpoint
 * and persists them idempotently.
 *
 * <p>Deliberately not wrapped in a single {@code @Transactional} spanning the whole loop:
 * that would hold a DB connection open across network calls. Instead each repository write
 * is its own transaction, and idempotency (unique {@code plaid_transaction_id}) makes a
 * re-run after a partial failure safe — the cursor only advances once a page is applied.
 */
@Service
public class TransactionSyncService {

    private static final Logger log = LoggerFactory.getLogger(TransactionSyncService.class);

    private final PlaidGateway plaidGateway;
    private final PlaidItemRepository itemRepository;
    private final BankTransactionRepository txnRepository;

    public TransactionSyncService(PlaidGateway plaidGateway,
                                  PlaidItemRepository itemRepository,
                                  BankTransactionRepository txnRepository) {
        this.plaidGateway = plaidGateway;
        this.itemRepository = itemRepository;
        this.txnRepository = txnRepository;
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
                    upsert(item, t);
                    changed++;
                }
            }
            if (resp.getModified() != null) {
                for (com.plaid.client.model.Transaction t : resp.getModified()) {
                    upsert(item, t);
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

    /** Insert-or-update a single transaction, keyed on the Plaid transaction id. */
    private void upsert(PlaidItem item, com.plaid.client.model.Transaction t) {
        BankTransaction e = txnRepository.findByPlaidTransactionId(t.getTransactionId())
                .orElseGet(BankTransaction::new);

        if (e.getId() == null) {
            e.setId(UUID.randomUUID());
            e.setPlaidTransactionId(t.getTransactionId());
            e.setPlaidItem(item);
            e.setUserId(item.getUserId());
        }

        e.setMerchantRaw(t.getName());
        e.setMerchantName(t.getMerchantName());
        e.setAmount(t.getAmount() != null ? BigDecimal.valueOf(t.getAmount()) : BigDecimal.ZERO);
        e.setIsoCurrency(t.getIsoCurrencyCode());
        e.setTxnDate(t.getDate());

        PersonalFinanceCategory pfc = t.getPersonalFinanceCategory();
        e.setPlaidCategory(pfc != null ? pfc.getPrimary() : null);

        Location loc = t.getLocation();
        if (loc != null) {
            e.setLocationCity(loc.getCity());
            e.setLocationRegion(loc.getRegion());
        }

        e.setPending(Boolean.TRUE.equals(t.getPending()));

        txnRepository.save(e);
        // Step 3 will publish a TransactionIngested event here for newly-added rows.
    }
}
