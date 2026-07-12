package com.impactbudget.categorization;

import com.impactbudget.common.TransactionIngested;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link TransactionIngested} events and runs the impact-scoring pipeline
 * (normalize → cache → Claude → curated overrides → persist → publish TransactionScored).
 */
@Component
class TransactionIngestedListener {

    private final CategorizationService categorizationService;

    TransactionIngestedListener(CategorizationService categorizationService) {
        this.categorizationService = categorizationService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.transactions-ingested}",
            groupId = "categorization")
    void onTransactionIngested(TransactionIngested event) {
        categorizationService.categorize(event);
    }
}
