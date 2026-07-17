package com.impactbudget.ingestion;

import com.impactbudget.common.TransactionIngested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionUpserterTest {

    @Mock
    BankTransactionRepository txnRepository;
    @Mock
    TransactionEventPublisher eventPublisher;

    @InjectMocks
    TransactionUpserter upserter;

    private PlaidItem item() {
        PlaidItem item = new PlaidItem();
        item.setId(UUID.randomUUID());
        item.setPlaidItemId("item-1");
        item.setUserId("user-1");
        item.setAccessToken("access-1");
        return item;
    }

    private com.plaid.client.model.Transaction plaidTxn(String id, double amount) {
        return new com.plaid.client.model.Transaction()
                .transactionId(id)
                .name("TST*SQ*LOCAL COFFEE 12345")
                .merchantName("Local Coffee")
                .amount(amount)
                .isoCurrencyCode("USD")
                .date(LocalDate.of(2026, 7, 1))
                .pending(false);
    }

    @Test
    void newTransactionIsInsertedWithGeneratedIdAndEmitsEvent() {
        when(txnRepository.findByPlaidTransactionId("txn-1")).thenReturn(Optional.empty());

        boolean isNew = upserter.upsert(item(), plaidTxn("txn-1", 4.50));

        assertThat(isNew).isTrue();

        ArgumentCaptor<BankTransaction> saved = ArgumentCaptor.forClass(BankTransaction.class);
        verify(txnRepository).save(saved.capture());
        BankTransaction row = saved.getValue();
        assertThat(row.getId()).isNotNull();
        assertThat(row.getPlaidTransactionId()).isEqualTo("txn-1");
        assertThat(row.getUserId()).isEqualTo("user-1");
        assertThat(row.getMerchantRaw()).isEqualTo("TST*SQ*LOCAL COFFEE 12345");
        assertThat(row.getAmount()).isEqualByComparingTo("4.50");

        // A new row enqueues exactly one TransactionIngested event (to the outbox).
        ArgumentCaptor<TransactionIngested> event = ArgumentCaptor.forClass(TransactionIngested.class);
        verify(eventPublisher).publishIngested(event.capture());
        assertThat(event.getValue().transactionId()).isEqualTo(row.getId());
        assertThat(event.getValue().userId()).isEqualTo("user-1");
    }

    @Test
    void redeliveredTransactionUpdatesExistingRowWithoutEmittingEvent() {
        BankTransaction existing = new BankTransaction();
        UUID existingId = UUID.randomUUID();
        existing.setId(existingId);
        existing.setPlaidTransactionId("txn-1");
        when(txnRepository.findByPlaidTransactionId("txn-1")).thenReturn(Optional.of(existing));

        boolean isNew = upserter.upsert(item(), plaidTxn("txn-1", 9.99));

        assertThat(isNew).isFalse();

        ArgumentCaptor<BankTransaction> saved = ArgumentCaptor.forClass(BankTransaction.class);
        verify(txnRepository).save(saved.capture());
        // Same row — idempotent: no duplicate, id unchanged, amount updated.
        assertThat(saved.getValue().getId()).isEqualTo(existingId);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("9.99");

        // A modification of an existing row must NOT re-emit an ingested event.
        verify(eventPublisher, never()).publishIngested(any());
    }
}
