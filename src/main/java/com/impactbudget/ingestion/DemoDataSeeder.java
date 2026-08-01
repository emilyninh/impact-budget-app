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
 * <p>Additive &amp; idempotent: each transaction has a stable content-based id, so re-running seeds
 * only the rows that don't exist yet — adding new demo merchants doesn't require wiping the DB. Runs
 * after {@code CuratedMerchantSeeder} (@Order) so curated/B-Corp scores are in place first.
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
        PlaidItem item = itemRepository.findByPlaidItemId(DEMO_ITEM).orElseGet(() -> {
            PlaidItem fresh = new PlaidItem();
            fresh.setId(UUID.randomUUID());
            fresh.setUserId(DEMO_USER);
            fresh.setPlaidItemId(DEMO_ITEM);
            fresh.setAccessToken("demo");
            fresh.setInstitutionName("Demo Bank");
            return itemRepository.save(fresh);
        });

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
        // Last month — mixed, with the local grocery + an unknown sustainable brand appearing.
        addAll(demos, now.minusMonths(1),
                d("STARBUCKS STORE 452", "Starbucks", "4.95"),
                d("TRADER JOES 210", "Trader Joe's", "41.15"),
                d("REI #45", "REI", "89.00"),
                d("SQ*BLUE BOTTLE COFFEE", "Blue Bottle", "6.50"),
                d("BOMBAS.COM", "Bombas", "36.00"),
                d("ZARA USA 12", "Zara", "59.90"),
                d("PORTLAND FARMERS MARKET", "Portland Farmers Market", "28.00", "FOOD_AND_DRINK"),
                d("ALBERTA COOP GROCERY", "Alberta Co-op Grocery", "54.10", "FOOD_AND_DRINK"),
                d("WWW.SIMPLEECOLOGY.COM", "Simpleecology", "38.40", "GENERAL_MERCHANDISE"),
                d("TONYS CHOCOLONELY", "Tony's Chocolonely", "5.49"));
        // This month — leaning local / sustainable, featuring the local grocery, farmers market, and
        // the two unknown sustainable brands the website enricher discovers.
        addAll(demos, now,
                d("PATAGONIA.COM", "Patagonia", "118.00"),
                d("REI #45", "REI", "54.25"),
                d("TST*ROSIES CORNER CAFE", "Rosie's Corner Cafe", "11.75"),
                d("PORTLAND FARMERS MARKET", "Portland Farmers Market", "33.50", "FOOD_AND_DRINK"),
                d("ALBERTA COOP GROCERY", "Alberta Co-op Grocery", "63.20", "FOOD_AND_DRINK"),
                d("WWW.SIMPLEECOLOGY.COM", "Simpleecology", "42.75", "GENERAL_MERCHANDISE"),
                d("SP LINA LENNOX", "Lina Lennox", "89.60", "GENERAL_MERCHANDISE"),
                // Sustainable brands curated in V16 (score + clean name via CuratedOverrideService).
                d("OSEA MALIBU", "OSEA", "48.00", "GENERAL_MERCHANDISE"),
                d("AVOCADO MATTRESS", "Avocado Mattress", "149.00", "GENERAL_MERCHANDISE"),
                d("THRIVE MARKET", "Thrive Market", "67.50", "FOOD_AND_DRINK"),
                d("ALLBIRDS", "Allbirds", "98.00"),
                d("NUMI ORGANIC TEA", "Numi Organic Tea", "9.25"),
                d("TRADER JOES 210", "Trader Joe's", "52.40"),
                d("KLEAN KANTEEN", "Klean Kanteen", "29.00"),
                d("STARBUCKS STORE 452", "Starbucks", "5.25"));

        int seeded = 0;
        for (Demo demo : demos) {
            // Stable content-based id → re-running seeds only new rows (additive, no wipe needed).
            String plaidTxnId = "demo-" + slug(demo.merchantRaw()) + "-" + demo.date();
            if (txnRepository.findByPlaidTransactionId(plaidTxnId).isPresent()) {
                continue;
            }
            UUID txnId = UUID.randomUUID();
            BankTransaction txn = new BankTransaction();
            txn.setId(txnId);
            txn.setPlaidTransactionId(plaidTxnId);
            txn.setPlaidItem(item);
            txn.setUserId(DEMO_USER);
            txn.setMerchantRaw(demo.merchantRaw());
            txn.setMerchantName(demo.merchantName());
            txn.setAmount(demo.amount());
            txn.setIsoCurrency("USD");
            txn.setTxnDate(demo.date());
            txn.setPlaidCategory(demo.sourceCategory());
            txn.setLocationCity("Portland");
            txn.setLocationRegion("OR");
            txnRepository.save(txn);

            eventPublisher.publishIngested(new TransactionIngested(
                    txnId, DEMO_USER, demo.merchantRaw(), demo.merchantName(),
                    demo.amount(), "USD", demo.date(), "Portland", "OR",
                    demo.sourceCategory(), null, "Demo Bank", null));
            seeded++;
        }

        log.info("Demo seed: {} new sample transactions published for {} ({} already present)",
                seeded, DEMO_USER, demos.size() - seeded);
    }

    private void addAll(List<Demo> out, YearMonth month, Demo... rows) {
        int day = 3;
        for (Demo r : rows) {
            out.add(new Demo(r.merchantRaw(), r.merchantName(), r.amount(), r.sourceCategory(),
                    month.atDay(Math.min(day, month.lengthOfMonth()))));
            day += 3;
        }
    }

    private static Demo d(String merchantRaw, String merchantName, String amount) {
        return new Demo(merchantRaw, merchantName, new BigDecimal(amount), null, null);
    }

    /** Overload with a Plaid PFC primary so demo rows categorize like real Plaid data. */
    private static Demo d(String merchantRaw, String merchantName, String amount, String sourceCategory) {
        return new Demo(merchantRaw, merchantName, new BigDecimal(amount), sourceCategory, null);
    }

    /** Lowercase-alphanumeric slug of a descriptor, for stable demo transaction ids. */
    private static String slug(String raw) {
        return raw.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private record Demo(String merchantRaw, String merchantName, BigDecimal amount,
                        String sourceCategory, LocalDate date) {
    }
}
