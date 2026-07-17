package com.impactbudget.common;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publishes unsent {@link OutboxEvent}s to Kafka on a fixed schedule and stamps them
 * published. Runs after the producing transaction has committed, so an event that made it to
 * the outbox is guaranteed to reach Kafka (at-least-once — consumers are idempotent).
 *
 * <p>A row whose payload can't be deserialized (e.g. a class was renamed) is skipped so it
 * can't block the queue; a transient Kafka failure stops the batch so the whole thing is
 * retried on the next tick rather than being dropped.
 */
@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxPayloadCodec codec;

    OutboxRelay(OutboxEventRepository repository, KafkaTemplate<String, Object> kafkaTemplate,
                OutboxPayloadCodec codec, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.codec = codec;
        Gauge.builder("outbox.pending", repository, OutboxEventRepository::countByPublishedAtIsNull)
                .description("Outbox events awaiting publication to Kafka")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:1000}")
    void publishPending() {
        List<OutboxEvent> batch = repository.findTop200ByPublishedAtIsNullOrderByCreatedAtAsc();
        if (batch.isEmpty()) {
            return;
        }
        int sent = 0;
        for (OutboxEvent event : batch) {
            Object payload;
            try {
                payload = codec.deserialize(event.getAggregateType(), event.getPayload());
            } catch (Exception poison) {
                log.error("Skipping un-deserializable outbox event {} ({})", event.getId(), poison.toString());
                continue;   // don't let a poison row block the rest of the queue
            }
            try {
                kafkaTemplate.send(event.getTopic(), event.getMsgKey(), payload)
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                event.setPublishedAt(Instant.now());
                repository.save(event);
                sent++;
            } catch (Exception transientFailure) {
                // Broker unreachable / timeout — leave this and the rest unpublished; retry next tick.
                log.warn("Outbox publish paused after {} sent ({})", sent, transientFailure.toString());
                break;
            }
        }
        if (sent > 0) {
            log.debug("Outbox relay published {} event(s)", sent);
        }
    }
}
