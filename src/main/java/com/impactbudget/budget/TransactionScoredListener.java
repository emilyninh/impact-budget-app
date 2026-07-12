package com.impactbudget.budget;

import com.impactbudget.common.TransactionScored;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link TransactionScored} events and folds them into the budget projection.
 * Uses its own consumer group so it receives every event independently of the
 * categorization consumer (the "one event, two consumers" fan-out).
 */
@Component
class TransactionScoredListener {

    private final BudgetAggregateService aggregateService;

    TransactionScoredListener(BudgetAggregateService aggregateService) {
        this.aggregateService = aggregateService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.transactions-scored}",
            groupId = "budget")
    void onTransactionScored(TransactionScored event) {
        aggregateService.record(event);
    }
}
