package com.impactbudget.ingestion;

import com.impactbudget.common.AppKafkaProperties;
import com.impactbudget.common.TransactionIngested;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link TransactionIngested} events. Keyed by {@code userId} so all of a
 * user's transactions land on the same partition and are processed in order.
 */
@Component
public class TransactionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AppKafkaProperties props;

    public TransactionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, AppKafkaProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    public void publishIngested(TransactionIngested event) {
        kafkaTemplate.send(props.topics().transactionsIngested(), event.userId(), event);
    }
}
