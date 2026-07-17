package com.impactbudget.common;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    OutboxEventRepository repository;
    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    OutboxPayloadCodec codec;

    private OutboxRelay relay() {
        return new OutboxRelay(repository, kafkaTemplate, codec, new SimpleMeterRegistry());
    }

    private OutboxEvent row(String type, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.setId(UUID.randomUUID());
        e.setAggregateType(type);
        e.setTopic("transactions.scored");
        e.setMsgKey("user-1");
        e.setPayload(payload);
        return e;
    }

    @Test
    void publishesPendingEventAndMarksItPublished() throws Exception {
        OutboxEvent e = row("com.example.Evt", "{}");
        when(repository.findTop200ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(e));
        Object payload = new Object();
        when(codec.deserialize("com.example.Evt", "{}")).thenReturn(payload);
        when(kafkaTemplate.send("transactions.scored", "user-1", payload))
                .thenReturn(CompletableFuture.completedFuture(null));

        relay().publishPending();

        assertThat(e.getPublishedAt()).isNotNull();
        verify(repository).save(e);
    }

    @Test
    void skipsPoisonRowThatCannotBeDeserialized() throws Exception {
        OutboxEvent poison = row("com.example.Gone", "{}");
        when(repository.findTop200ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(poison));
        when(codec.deserialize(anyString(), anyString())).thenThrow(new ClassNotFoundException("gone"));

        relay().publishPending();

        // Poison row is left unpublished and does not block the relay (no send, no mark).
        assertThat(poison.getPublishedAt()).isNull();
        verify(kafkaTemplate, never()).send(anyString(), any(), any());
        verify(repository, never()).save(eq(poison));
    }
}
