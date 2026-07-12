package com.impactbudget.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application Kafka topic names, bound from {@code app.kafka.*}. Referenced by producers,
 * consumers, and the topic-creation config so names live in exactly one place.
 */
@ConfigurationProperties(prefix = "app.kafka")
public record AppKafkaProperties(Topics topics) {

    public record Topics(String transactionsIngested, String transactionsScored) {
    }
}
