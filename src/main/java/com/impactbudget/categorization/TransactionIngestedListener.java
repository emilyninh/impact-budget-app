package com.impactbudget.categorization;

import com.impactbudget.common.TransactionIngested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link TransactionIngested} events for the categorization module.
 *
 * <p>Step 3 scaffold: currently just logs each event to prove the end-to-end flow
 * (Plaid sync → Kafka → consumer). Step 4 replaces the log with the real scoring
 * pipeline (merchant normalization → cache → Claude → curated overrides).
 */
@Component
class TransactionIngestedListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionIngestedListener.class);

    @KafkaListener(
            topics = "${app.kafka.topics.transactions-ingested}",
            groupId = "categorization")
    void onTransactionIngested(TransactionIngested event) {
        log.info("categorization received TransactionIngested: txn={} merchant='{}' amount={}",
                event.transactionId(), event.merchantRaw(), event.amount());
    }
}
