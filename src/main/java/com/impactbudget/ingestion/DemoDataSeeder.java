package com.impactbudget.ingestion;

import com.impactbudget.common.DemoIds;
import com.impactbudget.common.TransactionIngested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dev-only demo data. When {@code demo.seed-enabled=true} (set in docker-compose for local
 * runs, off by default and in tests), seeds a handful of sample transactions across the last
 * three months for {@code demo-user} and publishes them through the real pipeline
 * (Kafka → categorization → budget → dashboard) so the UI has something to show before Plaid
 * is connected.
 *
 * <p>Idempotent: skips entirely if the demo Plaid item already exists. Runs after
 * {@code CuratedMerchantSeeder} (@Order) so curated/B-Corp scores are in place first.
 *
 * <p>To switch to real Plaid data later: set {@code DEMO_SEED_ENABLED=false} and, for a clean
 * slate, {@code docker compose down -v && docker compose up -d}.
 */
@Component
@ConditionalOnProperty(name = "demo.seed-enabled", havingValue = "true")
@Order(20)
class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_USER = DemoIds.DEMO_USER_ID;
    private static final String DEMO_ITEM = "demo-item";

    private final PlaidItemRepository itemRepository;
    private final BankTransactionRepository txnRepository;
    private final TransactionEventPublisher eventPublisher;

    DemoDataSeeder(PlaidItemRepository itemRepository,
                   BankTransactionRepository txnRepository,
                   TransactionEventPublisher eventPublisher) {
        this.itemRepository = itemRepository;
        this.txnRepository = txnRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (itemRepository.findByPlaidItemId(DEMO_ITEM).isPresent()) {
            return; // already seeded
        }

        PlaidItem item = new PlaidItem();
        item.setId(UUID.randomUUID());
        item.setUserId(DEMO_USER);
        item.setPlaidItemId(DEMO_ITEM);
        item.setAccessToken("demo");
        item.setInstitutionName("Demo Bank");
        itemRepository.save(item);

        YearMonth now = YearMonth.now();
        List<Demo> demos = new ArrayList<>();
        // Two months ago — heavier on chains / fast fashion.
        addAll(demos, now.minusMonths(2),
                d("STARBUCKS STORE 452", "Starbucks", "5.75"),
                d("MCDONALDS F1123", "McDonald's", "8.40"),
                d("WALMART SUPERCENTER", "Walmart", "132.10"),
                d("AMAZON.COM*A1B2C3", "Amazon", "47.99"),
                d("SHEIN.COM", "Shein", "38.20"),
                d("H&M 0345", "H&M", "24.99"),
                d("TST*ROSIES CORNER CAFE", "Rosie's Corner Cafe", "12.25"),
                d("WHOLE FOODS MKT 101", "Whole Foods", "64.30"));
        // Last month — mixed.
        addAll(demos, now.minusMonths(1),
                d("STARBUCKS STORE 452", "Starbucks", "4.95"),
                d("TRADER JOES 210", "Trader Joe's", "41.15"),
                d("REI #45", "REI", "89.00"),
                d("SQ*BLUE BOTTLE COFFEE", "Blue Bottle", "6.50"),
                d("BOMBAS.COM", "Bombas", "36.00"),
                d("ZARA USA 12", "Zara", "59.90"),
                d("PORTLAND FARMERS MARKET", "Portland Farmers Market", "28.00"),
                d("TONYS CHOCOLONELY", "Tony's Chocolonely", "5.49"));
        // This month — leaning local / sustainable, to show upward goal progress.
        addAll(demos, now,
                d("PATAGONIA.COM", "Patagonia", "118.00"),
                d("REI #45", "REI", "54.25"),
                d("TST*ROSIES CORNER CAFE", "Rosie's Corner Cafe", "11.75"),
                d("PORTLAND FARMERS MARKET", "Portland Farmers Market", "33.50"),
                d("ALLBIRDS", "Allbirds", "98.00"),
                d("NUMI ORGANIC TEA", "Numi Organic Tea", "9.25"),
                d("TRADER JOES 210", "Trader Joe's", "52.40"),
                d("KLEAN KANTEEN", "Klean Kanteen", "29.00"),
                d("STARBUCKS STORE 452", "Starbucks", "5.25"));

        int i = 0;
        for (Demo demo : demos) {
            UUID txnId = UUID.randomUUID();
            BankTransaction txn = new BankTransaction();
            txn.setId(txnId);
            txn.setPlaidTransactionId("demo-" + (i++));
            txn.setPlaidItem(item);
            txn.setUserId(DEMO_USER);
            txn.setMerchantRaw(demo.merchantRaw());
            txn.setMerchantName(demo.merchantName());
            txn.setAmount(demo.amount());
            txn.setIsoCurrency("USD");
            txn.setTxnDate(demo.date());
            txn.setLocationCity("Portland");
            txn.setLocationRegion("OR");
            txnRepository.save(txn);

            eventPublisher.publishIngested(new TransactionIngested(
                    txnId, DEMO_USER, demo.merchantRaw(), demo.merchantName(),
                    demo.amount(), "USD", demo.date(), "Portland", "OR"));
        }

        log.info("Demo seed: {} sample transactions published for {}", demos.size(), DEMO_USER);
    }

    private void addAll(List<Demo> out, YearMonth month, Demo... rows) {
        int day = 3;
        for (Demo r : rows) {
            out.add(new Demo(r.merchantRaw(), r.merchantName(), r.amount(),
                    month.atDay(Math.min(day, month.lengthOfMonth()))));
            day += 3;
        }
    }

    private static Demo d(String merchantRaw, String merchantName, String amount) {
        return new Demo(merchantRaw, merchantName, new BigDecimal(amount), null);
    }

    private record Demo(String merchantRaw, String merchantName, BigDecimal amount, LocalDate date) {
    }
}
