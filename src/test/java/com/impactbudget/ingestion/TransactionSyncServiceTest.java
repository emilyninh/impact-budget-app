package com.impactbudget.ingestion;

import com.plaid.client.model.RemovedTransaction;
import com.plaid.client.model.TransactionsSyncResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionSyncServiceTest {

    @Mock
    PlaidGateway plaidGateway;
    @Mock
    PlaidItemRepository itemRepository;
    @Mock
    BankTransactionRepository txnRepository;
    @Mock
    TransactionUpserter upserter;

    @InjectMocks
    TransactionSyncService service;

    private PlaidItem item() {
        PlaidItem item = new PlaidItem();
        item.setId(UUID.randomUUID());
        item.setPlaidItemId("item-1");
        item.setUserId("user-1");
        item.setAccessToken("access-1");
        return item;
    }

    private com.plaid.client.model.Transaction plaidTxn(String id) {
        return new com.plaid.client.model.Transaction()
                .transactionId(id)
                .name("STORE")
                .amount(4.50)
                .date(LocalDate.of(2026, 7, 1))
                .pending(false);
    }

    @Test
    void drainsAddedTransactionsDelegatesUpsertsAndAdvancesCursor() {
        PlaidItem item = item();
        when(plaidGateway.syncTransactions("access-1", null)).thenReturn(
                new TransactionsSyncResponse()
                        .added(List.of(plaidTxn("txn-1"), plaidTxn("txn-2")))
                        .modified(List.of())
                        .removed(List.of())
                        .nextCursor("cursor-A")
                        .hasMore(false));

        int changed = service.sync(item);

        assertThat(changed).isEqualTo(2);
        // Each added transaction is upserted through the transactional collaborator.
        verify(upserter, times(2)).upsert(eq(item), any());
        // Cursor persisted so the next sync resumes from here.
        assertThat(item.getTransactionsCursor()).isEqualTo("cursor-A");
        verify(itemRepository).save(item);
    }

    @Test
    void removedTransactionsAreDeleted() {
        PlaidItem item = item();
        when(plaidGateway.syncTransactions(eq("access-1"), any())).thenReturn(
                new TransactionsSyncResponse()
                        .added(List.of())
                        .modified(List.of())
                        .removed(List.of(new RemovedTransaction().transactionId("gone-1")))
                        .nextCursor("cursor-B")
                        .hasMore(false));

        int changed = service.sync(item);

        assertThat(changed).isEqualTo(1);
        verify(txnRepository).deleteByPlaidTransactionId("gone-1");
    }
}
