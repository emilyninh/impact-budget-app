package com.impactbudget.common;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumes the {@code .DLT} topics and records each failed event to the {@code dead_letter}
 * table so failures are queryable and replayable, rather than languishing unseen in a topic.
 */
@Component
class DeadLetterListener {

    private final DeadLetterService deadLetterService;

    DeadLetterListener(DeadLetterService deadLetterService) {
        this.deadLetterService = deadLetterService;
    }

    @KafkaListener(
            topics = {
                    "${app.kafka.topics.transactions-ingested}.DLT",
                    "${app.kafka.topics.transactions-scored}.DLT"
            },
            groupId = "dlq-recorder")
    void onDeadLetter(ConsumerRecord<String, Object> record,
                      @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String errorMessage) {
        deadLetterService.record(record.topic(), record.key(), record.value(), errorMessage);
    }
}
