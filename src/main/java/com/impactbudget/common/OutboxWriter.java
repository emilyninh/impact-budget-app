package com.impactbudget.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Writes a domain event to the outbox table. Intentionally has no transaction of its own so
 * the insert joins the caller's transaction — that atomicity (domain row + outbox row commit
 * together) is the whole point of the pattern. Call it only from a {@code @Transactional}
 * method that also performs the domain write.
 */
@Component
public class OutboxWriter {

    private final OutboxEventRepository repository;
    private final ObjectMapper mapper;

    public OutboxWriter(OutboxEventRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public void enqueue(String topic, String key, Object event) {
        OutboxEvent row = new OutboxEvent();
        row.setId(UUID.randomUUID());
        row.setAggregateType(event.getClass().getName());
        row.setTopic(topic);
        row.setMsgKey(key);
        row.setPayload(serialize(event));
        repository.save(row);
    }

    private String serialize(Object event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event " + event.getClass(), e);
        }
    }
}
