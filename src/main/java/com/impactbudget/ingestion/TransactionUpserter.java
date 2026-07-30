package com.impactbudget.ingestion;

import com.impactbudget.common.TransactionIngested;
import com.plaid.client.model.Location;
import com.plaid.client.model.PersonalFinanceCategory;
import com.plaid.client.model.TransactionCounterparty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Persists a single Plaid transaction and enqueues its {@link TransactionIngested} event in
 * one transaction (transactional outbox). Separate bean so {@code @Transactional} applies
 * (self-invocation from {@link TransactionSyncService}'s loop would bypass the proxy), and so
 * the transaction stays short — the Plaid network call happens in the caller, not here.
 */
@Component
class TransactionUpserter {

    private final BankTransactionRepository txnRepository;
    private final TransactionEventPublisher eventPublisher;

    TransactionUpserter(BankTransactionRepository txnRepository, TransactionEventPublisher eventPublisher) {
        this.txnRepository = txnRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Insert-or-update a transaction keyed on the Plaid id; returns whether a new row was created. */
    @Transactional
    boolean upsert(PlaidItem item, com.plaid.client.model.Transaction t) {
        BankTransaction e = txnRepository.findByPlaidTransactionId(t.getTransactionId())
                .orElseGet(BankTransaction::new);

        boolean isNew = e.getId() == null;
        if (isNew) {
            e.setId(UUID.randomUUID());
            e.setPlaidTransactionId(t.getTransactionId());
            e.setPlaidItem(item);
            e.setUserId(item.getUserId());
        }

        e.setMerchantRaw(t.getName());
        e.setMerchantName(t.getMerchantName());
        e.setAmount(t.getAmount() != null ? BigDecimal.valueOf(t.getAmount()) : BigDecimal.ZERO);
        e.setIsoCurrency(t.getIsoCurrencyCode());
        e.setTxnDate(t.getDate());

        PersonalFinanceCategory pfc = t.getPersonalFinanceCategory();
        e.setPlaidCategory(pfc != null ? pfc.getPrimary() : null);
        e.setPlaidCategoryDetailed(pfc != null ? pfc.getDetailed() : null);

        // Merchant web identity — Plaid's top-level website, else the primary counterparty's.
        e.setMerchantWebsite(StringUtils.hasText(t.getWebsite()) ? t.getWebsite() : counterpartyWebsite(t));
        e.setMerchantEntityId(t.getMerchantEntityId());

        Location loc = t.getLocation();
        if (loc != null) {
            e.setLocationCity(loc.getCity());
            e.setLocationRegion(loc.getRegion());
        }

        e.setPending(Boolean.TRUE.equals(t.getPending()));

        txnRepository.save(e);

        // Publish only for newly-inserted rows so a modification re-sync doesn't re-trigger
        // scoring. The outbox insert commits atomically with the row above.
        if (isNew) {
            eventPublisher.publishIngested(toEvent(e));
        }
        return isNew;
    }

    private TransactionIngested toEvent(BankTransaction e) {
        return new TransactionIngested(
                e.getId(),
                e.getUserId(),
                e.getMerchantRaw(),
                e.getMerchantName(),
                e.getAmount(),
                e.getIsoCurrency(),
                e.getTxnDate(),
                e.getLocationCity(),
                e.getLocationRegion(),
                e.getPlaidCategory(),           // Plaid PFC primary — taxonomy hint
                e.getPlaidCategoryDetailed(),    // Plaid PFC detailed — refines the taxonomy
                e.getPlaidItem().getInstitutionName(),   // source bank (in-txn: item is loaded)
                e.getMerchantWebsite());         // merchant domain for web-signal scoring
    }

    /** The website of the first counterparty that has one, or null. */
    private static String counterpartyWebsite(com.plaid.client.model.Transaction t) {
        List<TransactionCounterparty> parties = t.getCounterparties();
        if (parties == null) {
            return null;
        }
        return parties.stream()
                .map(TransactionCounterparty::getWebsite)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }
}
