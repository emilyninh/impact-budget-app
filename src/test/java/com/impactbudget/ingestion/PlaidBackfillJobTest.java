package com.impactbudget.ingestion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaidBackfillJobTest {

    @Mock
    PlaidItemRepository itemRepository;
    @Mock
    TransactionSyncService syncService;

    @InjectMocks
    PlaidBackfillJob job;

    private PlaidItem item(Instant backfillUntil) {
        PlaidItem i = new PlaidItem();
        i.setId(UUID.randomUUID());
        i.setPlaidItemId("item-1");
        i.setAccessToken("access-sandbox-x");
        i.setBackfillUntil(backfillUntil);
        return i;
    }

    @Test
    void closesWindowWhenTransactionsArrive() {
        PlaidItem i = item(Instant.now().plusSeconds(120));
        when(itemRepository.findByBackfillUntilIsNotNull()).thenReturn(List.of(i));
        when(syncService.sync(i)).thenReturn(3);

        job.backfillPendingItems();

        assertThat(i.getBackfillUntil()).isNull();   // data arrived — stop backfilling
        verify(itemRepository).save(i);
    }

    @Test
    void keepsWindowOpenWhileStillEmpty() {
        PlaidItem i = item(Instant.now().plusSeconds(120));
        when(itemRepository.findByBackfillUntilIsNotNull()).thenReturn(List.of(i));
        when(syncService.sync(i)).thenReturn(0);

        job.backfillPendingItems();

        assertThat(i.getBackfillUntil()).isNotNull();   // keep retrying next tick
        verify(itemRepository, never()).save(i);
    }

    @Test
    void givesUpAfterWindowExpires() {
        PlaidItem i = item(Instant.now().minusSeconds(1));
        when(itemRepository.findByBackfillUntilIsNotNull()).thenReturn(List.of(i));

        job.backfillPendingItems();

        assertThat(i.getBackfillUntil()).isNull();     // window elapsed — give up
        verify(syncService, never()).sync(any());
        verify(itemRepository).save(i);
    }
}
