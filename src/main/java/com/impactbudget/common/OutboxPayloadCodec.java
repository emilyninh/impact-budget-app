package com.impactbudget.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Deserializes an outbox payload back into its event object using the stored FQCN. The data
 * is app-authored and fully trusted, so reflective type resolution is safe here.
 */
@Component
public class OutboxPayloadCodec {

    private final ObjectMapper mapper;

    public OutboxPayloadCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Object deserialize(String aggregateType, String payload) throws Exception {
        Class<?> type = Class.forName(aggregateType);
        return mapper.readValue(payload, type);
    }
}
