package com.impactbudget.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxWriterTest {

    @Mock
    OutboxEventRepository repository;

    @Test
    void enqueueSerializesEventWithItsClassNameAndTopicKey() {
        OutboxWriter writer = new OutboxWriter(repository, new ObjectMapper().findAndRegisterModules());
        UUID txnId = UUID.randomUUID();
        TransactionScored event = new TransactionScored(
                txnId, "user-1", "Local Coffee", new BigDecimal("4.50"), LocalDate.of(2026, 7, 1),
                "Eating Out", 85, true, 60, List.of(), 0.7, "LLM");

        writer.enqueue("transactions.scored", "user-1", event);

        ArgumentCaptor<OutboxEvent> row = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(row.capture());
        assertThat(row.getValue().getTopic()).isEqualTo("transactions.scored");
        assertThat(row.getValue().getMsgKey()).isEqualTo("user-1");
        assertThat(row.getValue().getAggregateType()).isEqualTo(TransactionScored.class.getName());
        assertThat(row.getValue().getPayload()).contains("\"category\":\"Eating Out\"");
        assertThat(row.getValue().getId()).isNotNull();
    }
}
