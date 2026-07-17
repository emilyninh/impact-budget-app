package com.impactbudget.ingestion;

import com.impactbudget.common.AppKafkaProperties;
import com.impactbudget.common.OutboxWriter;
import com.impactbudget.common.TransactionIngested;
import org.springframework.stereotype.Component;

/**
 * Enqueues {@link TransactionIngested} events to the transactional outbox (not Kafka
 * directly), keyed by {@code userId} so a user's transactions stay ordered on one partition.
 * Must be called inside the same transaction as the {@code bank_transaction} write so the row
 * and its event commit atomically; the {@code OutboxRelay} then publishes to Kafka.
 */
@Component
public class TransactionEventPublisher {

    private final OutboxWriter outboxWriter;
    private final AppKafkaProperties props;

    public TransactionEventPublisher(OutboxWriter outboxWriter, AppKafkaProperties props) {
        this.outboxWriter = outboxWriter;
        this.props = props;
    }

    public void publishIngested(TransactionIngested event) {
        outboxWriter.enqueue(props.topics().transactionsIngested(), event.userId(), event);
    }
}
