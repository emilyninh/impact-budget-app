package com.impactbudget.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Records dead-lettered events and replays them back onto their original topic on demand. */
@Service
public class DeadLetterService {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterService.class);
    private static final String DLT_SUFFIX = ".DLT";

    private final DeadLetterRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxPayloadCodec codec;
    private final ObjectMapper mapper;
    private final MeterRegistry meterRegistry;

    public DeadLetterService(DeadLetterRepository repository, KafkaTemplate<String, Object> kafkaTemplate,
                             OutboxPayloadCodec codec, ObjectMapper mapper, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.codec = codec;
        this.mapper = mapper;
        this.meterRegistry = meterRegistry;
        Gauge.builder("dlq.pending", repository, DeadLetterRepository::countByReplayedAtIsNull)
                .description("Dead-lettered events not yet replayed")
                .register(meterRegistry);
    }

    /** Persist a failed record for visibility/replay. */
    public void record(String dltTopic, String key, Object payload, String errorMessage) {
        DeadLetter dl = new DeadLetter();
        dl.setId(UUID.randomUUID());
        dl.setDltTopic(dltTopic);
        dl.setOriginalTopic(originalTopicOf(dltTopic));
        dl.setAggregateType(payload.getClass().getName());
        dl.setMsgKey(key);
        dl.setPayload(serialize(payload));
        dl.setErrorMessage(errorMessage);
        repository.save(dl);
        meterRegistry.counter("dlq.received", "topic", dl.getOriginalTopic()).increment();
        log.warn("Dead-lettered event from {} (key={}): {}", dl.getOriginalTopic(), key, errorMessage);
    }

    /** Re-publish every not-yet-replayed dead letter to its original topic. Returns the count. */
    public int replayAll() {
        List<DeadLetter> pending = repository.findByReplayedAtIsNullOrderByCreatedAtAsc();
        int replayed = 0;
        for (DeadLetter dl : pending) {
            try {
                Object payload = codec.deserialize(dl.getAggregateType(), dl.getPayload());
                kafkaTemplate.send(dl.getOriginalTopic(), dl.getMsgKey(), payload).get(10, TimeUnit.SECONDS);
                dl.setReplayedAt(Instant.now());
                repository.save(dl);
                replayed++;
            } catch (Exception e) {
                log.error("Failed to replay dead letter {} ({})", dl.getId(), e.toString());
            }
        }
        if (replayed > 0) {
            meterRegistry.counter("dlq.replayed").increment(replayed);
            log.info("Replayed {} dead letter(s)", replayed);
        }
        return replayed;
    }

    private static String originalTopicOf(String dltTopic) {
        return dltTopic.endsWith(DLT_SUFFIX)
                ? dltTopic.substring(0, dltTopic.length() - DLT_SUFFIX.length())
                : dltTopic;
    }

    private String serialize(Object payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize dead letter payload", e);
        }
    }
}
