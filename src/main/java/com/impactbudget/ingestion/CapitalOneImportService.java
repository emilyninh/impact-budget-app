package com.impactbudget.ingestion;

import com.impactbudget.common.TransactionIngested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Imports a Capital One transaction CSV export and pushes each purchase through the same
 * pipeline as Plaid (categorization → budget → dashboard). An alternative ingestion path when
 * you'd rather upload a statement than link an account.
 *
 * <p>CSV columns: {@code Transaction Date, Posted Date, Card No., Description, Category, Debit,
 * Credit}. Only rows with a Debit (spending) are imported; Credits (payments/refunds) are
 * skipped. Capital One's own Category is passed as a scoring hint. Idempotent: a deterministic
 * id per row means re-uploading the same file doesn't duplicate.
 */
@Service
public class CapitalOneImportService {

    private static final Logger log = LoggerFactory.getLogger(CapitalOneImportService.class);

    private final PlaidItemRepository itemRepository;
    private final BankTransactionRepository txnRepository;
    private final TransactionEventPublisher eventPublisher;

    public CapitalOneImportService(PlaidItemRepository itemRepository,
                                   BankTransactionRepository txnRepository,
                                   TransactionEventPublisher eventPublisher) {
        this.itemRepository = itemRepository;
        this.txnRepository = txnRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Import all Debit rows for the given user. Returns the number of transactions imported. */
    public int importCsv(String userId, InputStream csv) throws IOException {
        PlaidItem item = importItemFor(userId);
        int imported = 0;
        int row = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            String line = reader.readLine();   // header
            while ((line = reader.readLine()) != null) {
                row++;
                String[] c = line.split(",", -1);
                if (c.length < 7) {
                    continue;   // malformed / blank line
                }
                String debit = c[5].trim();
                if (debit.isEmpty()) {
                    continue;   // a Credit row (payment/refund) — not spending
                }

                BigDecimal amount;
                LocalDate date;
                try {
                    amount = new BigDecimal(debit);
                    date = LocalDate.parse(c[0].trim());
                } catch (RuntimeException e) {
                    log.warn("Skipping unparseable CSV row {}: {}", row, e.toString());
                    continue;
                }

                String description = c[3].trim();
                String category = c[4].trim();
                // Deterministic per-file id → re-importing the same file is idempotent.
                String importId = "csv:" + userId + ":" + row;
                if (txnRepository.findByPlaidTransactionId(importId).isPresent()) {
                    continue;
                }

                UUID txnId = UUID.randomUUID();
                BankTransaction txn = new BankTransaction();
                txn.setId(txnId);
                txn.setPlaidTransactionId(importId);
                txn.setPlaidItem(item);
                txn.setUserId(userId);
                txn.setMerchantRaw(description);
                txn.setAmount(amount);
                txn.setIsoCurrency("USD");
                txn.setTxnDate(date);
                txn.setPlaidCategory(category.isEmpty() ? null : category);
                txnRepository.save(txn);

                eventPublisher.publishIngested(new TransactionIngested(
                        txnId, userId, description, null, amount, "USD", date, null, null,
                        category.isEmpty() ? null : category, null, "Capital One", null));
                imported++;
            }
        }

        log.info("CSV import: {} transactions imported for {}", imported, userId);
        return imported;
    }

    /** One synthetic Plaid item per user holds imported rows (bank_transaction requires an item). */
    private PlaidItem importItemFor(String userId) {
        String itemId = "csv-import:" + userId;
        return itemRepository.findByPlaidItemId(itemId).orElseGet(() -> {
            PlaidItem item = new PlaidItem();
            item.setId(UUID.randomUUID());
            item.setUserId(userId);
            item.setPlaidItemId(itemId);
            item.setAccessToken("csv");
            item.setInstitutionName("CSV import (Capital One)");
            return itemRepository.save(item);
        });
    }
}
