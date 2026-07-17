package com.impactbudget.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadLetterServiceTest {

    @Mock
    DeadLetterRepository repository;
    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    OutboxPayloadCodec codec;

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    private DeadLetterService service() {
        return new DeadLetterService(repository, kafkaTemplate, codec, mapper, meters);
    }

    private TransactionScored event() {
        return new TransactionScored(UUID.randomUUID(), "user-1", "Store", new BigDecimal("10.00"),
                LocalDate.of(2026, 7, 1), "Shopping", 40, false, 50, List.of(), 0.5, "LLM");
    }

    @Test
    void recordDerivesOriginalTopicAndCountsMetric() {
        service().record("transactions.scored.DLT", "user-1", event(), "boom");

        ArgumentCaptor<DeadLetter> dl = ArgumentCaptor.forClass(DeadLetter.class);
        verify(repository).save(dl.capture());
        assertThat(dl.getValue().getDltTopic()).isEqualTo("transactions.scored.DLT");
        assertThat(dl.getValue().getOriginalTopic()).isEqualTo("transactions.scored");
        assertThat(dl.getValue().getAggregateType()).isEqualTo(TransactionScored.class.getName());
        assertThat(dl.getValue().getErrorMessage()).isEqualTo("boom");
        assertThat(meters.counter("dlq.received", "topic", "transactions.scored").count()).isEqualTo(1.0);
    }

    @Test
    void replayAllRepublishesToOriginalTopicAndMarksReplayed() throws Exception {
        DeadLetter dl = new DeadLetter();
        dl.setId(UUID.randomUUID());
        dl.setDltTopic("transactions.scored.DLT");
        dl.setOriginalTopic("transactions.scored");
        dl.setAggregateType(TransactionScored.class.getName());
        dl.setMsgKey("user-1");
        dl.setPayload("{}");
        when(repository.findByReplayedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(dl));
        Object payload = event();
        when(codec.deserialize(TransactionScored.class.getName(), "{}")).thenReturn(payload);
        when(kafkaTemplate.send("transactions.scored", "user-1", payload))
                .thenReturn(CompletableFuture.completedFuture(null));

        int replayed = service().replayAll();

        assertThat(replayed).isEqualTo(1);
        assertThat(dl.getReplayedAt()).isNotNull();
        verify(kafkaTemplate).send(eq("transactions.scored"), eq("user-1"), any());
        verify(repository).save(dl);
    }
}
