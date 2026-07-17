package com.impactbudget.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka wiring. The producer/consumer factories are built explicitly so the JSON
 * serializers use Spring's configured {@link ObjectMapper} — which registers the
 * JavaTimeModule and so can (de)serialize {@code LocalDate}/{@code BigDecimal} in events.
 * The auto-configured serializers use a bare ObjectMapper that would fail on those types.
 */
@Configuration
class KafkaConfig {

    // --- Producer -------------------------------------------------------------

    @Bean
    ProducerFactory<String, Object> producerFactory(KafkaProperties props, ObjectMapper mapper,
                                                    SslBundles sslBundles, KafkaConnectionDetails connectionDetails) {
        var config = props.buildProducerProperties(sslBundles);
        // Honor the resolved broker address (e.g. Testcontainers' @ServiceConnection) rather
        // than the static spring.kafka.bootstrap-servers.
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getBootstrapServers());
        // Exactly-once-ish producer: no duplicates on retry, all in-sync replicas must ack.
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), new JsonSerializer<>(mapper));
    }

    @Bean
    KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        template.setObservationEnabled(true);   // emit producer spans for distributed tracing
        return template;
    }

    // --- Consumer -------------------------------------------------------------

    @Bean
    ConsumerFactory<String, Object> consumerFactory(KafkaProperties props, ObjectMapper mapper,
                                                    SslBundles sslBundles, KafkaConnectionDetails connectionDetails) {
        JsonDeserializer<Object> valueDeserializer = new JsonDeserializer<>(mapper);
        // Internal, fully-trusted event bus — every message originates from this app.
        valueDeserializer.trustedPackages("*");
        var config = props.buildConsumerProperties(sslBundles);
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getBootstrapServers());
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory, DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setObservationEnabled(true);   // consumer spans for tracing
        return factory;
    }

    /**
     * Retries a failing record a few times with exponential backoff, then routes it to the
     * topic's {@code .DLT} instead of dropping it (the default silently discards after retries).
     */
    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        // Retry 3 times, 500ms apart, then route the record to <topic>.DLT.
        FixedBackOff backOff = new FixedBackOff(500L, 3L);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    // --- Topics (auto-created on startup via KafkaAdmin) ----------------------

    @Bean
    NewTopic transactionsIngestedTopic(AppKafkaProperties props) {
        return TopicBuilder.name(props.topics().transactionsIngested()).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic transactionsScoredTopic(AppKafkaProperties props) {
        return TopicBuilder.name(props.topics().transactionsScored()).partitions(3).replicas(1).build();
    }

    // Dead-letter topics — the error handler routes exhausted records here (same partition count).
    @Bean
    NewTopic transactionsIngestedDlt(AppKafkaProperties props) {
        return TopicBuilder.name(props.topics().transactionsIngested() + ".DLT").partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic transactionsScoredDlt(AppKafkaProperties props) {
        return TopicBuilder.name(props.topics().transactionsScored() + ".DLT").partitions(3).replicas(1).build();
    }
}
