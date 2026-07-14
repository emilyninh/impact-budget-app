package com.impactbudget;

import com.impactbudget.budget.ScoredTransactionRepository;
import com.impactbudget.categorization.ImpactScore;
import com.impactbudget.categorization.ImpactScoreRepository;
import com.impactbudget.common.AppKafkaProperties;
import com.impactbudget.common.TransactionIngested;
import com.impactbudget.ingestion.BankTransaction;
import com.impactbudget.ingestion.BankTransactionRepository;
import com.impactbudget.ingestion.PlaidItem;
import com.impactbudget.ingestion.PlaidItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end flow across three modules and real infrastructure: publishing a
 * {@link TransactionIngested} event drives the categorization consumer (which writes an
 * {@link ImpactScore}) and, via the {@code TransactionScored} it emits, the budget consumer
 * (which records a {@code scored_transaction}). Runs against real Postgres/Kafka/Redis
 * containers; skipped when Docker isn't available.
 */
@SpringBootTest(properties = {
        // Deterministic & offline: neutral scorer, no external enrichment calls.
        "categorization.scoring.provider=none",
        "openfoodfacts.enabled=false",
        "wikidata.enabled=false"
})
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class CategorizationFlowIT {

    @Autowired
    KafkaTemplate<String, Object> kafka;
    @Autowired
    AppKafkaProperties topics;
    @Autowired
    PlaidItemRepository plaidItemRepository;
    @Autowired
    BankTransactionRepository bankTransactionRepository;
    @Autowired
    ImpactScoreRepository impactScoreRepository;
    @Autowired
    ScoredTransactionRepository scoredTransactionRepository;

    @Test
    void ingestedEventIsScoredAndAggregated() {
        // Arrange: persist the parent rows so impact_score's FK to bank_transaction holds
        // (in production these exist before the event is published).
        PlaidItem item = new PlaidItem();
        item.setId(UUID.randomUUID());
        item.setUserId("it-user");
        item.setPlaidItemId("it-item-" + UUID.randomUUID());
        item.setAccessToken("access");
        plaidItemRepository.save(item);

        UUID txnId = UUID.randomUUID();
        BankTransaction txn = new BankTransaction();
        txn.setId(txnId);
        txn.setPlaidTransactionId("it-txn-" + txnId);
        txn.setPlaidItem(item);
        txn.setUserId("it-user");
        txn.setMerchantRaw("TST*LOCAL CAFE 99");
        txn.setMerchantName("Local Cafe");
        txn.setAmount(new BigDecimal("12.00"));
        txn.setTxnDate(LocalDate.now());
        bankTransactionRepository.save(txn);

        // Act: publish the event the ingestion module would have emitted.
        TransactionIngested event = new TransactionIngested(
                txnId, "it-user", "TST*LOCAL CAFE 99", "Local Cafe",
                new BigDecimal("12.00"), "USD", LocalDate.now(), "Portland", "OR");
        kafka.send(topics.topics().transactionsIngested(), event.userId(), event);

        // Assert: both consumers processed it.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(impactScoreRepository.findByTransactionId(txnId)).isPresent();
            assertThat(scoredTransactionRepository.existsByTransactionId(txnId)).isTrue();
        });

        ImpactScore score = impactScoreRepository.findByTransactionId(txnId).orElseThrow();
        // No ANTHROPIC_API_KEY in the test env, so scoring takes the fallback path.
        assertThat(score.getSource()).isEqualTo("FALLBACK");
        assertThat(score.getUserId()).isEqualTo("it-user");
    }
}
