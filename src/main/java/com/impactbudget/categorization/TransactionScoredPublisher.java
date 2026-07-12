package com.impactbudget.categorization;

import com.impactbudget.common.AppKafkaProperties;
import com.impactbudget.common.TransactionScored;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes {@link TransactionScored} events, keyed by {@code userId}. */
@Component
public class TransactionScoredPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AppKafkaProperties props;

    public TransactionScoredPublisher(KafkaTemplate<String, Object> kafkaTemplate, AppKafkaProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    public void publishScored(TransactionScored event) {
        kafkaTemplate.send(props.topics().transactionsScored(), event.userId(), event);
    }
}
