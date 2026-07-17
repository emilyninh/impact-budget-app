package com.impactbudget.categorization;

import com.impactbudget.common.AppKafkaProperties;
import com.impactbudget.common.OutboxWriter;
import com.impactbudget.common.TransactionScored;
import org.springframework.stereotype.Component;

/**
 * Enqueues {@link TransactionScored} events to the transactional outbox, keyed by
 * {@code userId}. Called inside the same transaction as the {@code impact_score} write so the
 * score and its event commit atomically; the {@code OutboxRelay} publishes to Kafka.
 */
@Component
public class TransactionScoredPublisher {

    private final OutboxWriter outboxWriter;
    private final AppKafkaProperties props;

    public TransactionScoredPublisher(OutboxWriter outboxWriter, AppKafkaProperties props) {
        this.outboxWriter = outboxWriter;
        this.props = props;
    }

    public void publishScored(TransactionScored event) {
        outboxWriter.enqueue(props.topics().transactionsScored(), event.userId(), event);
    }
}
