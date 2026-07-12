package com.impactbudget.ingestion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives Plaid webhooks. For any {@code TRANSACTIONS} webhook (e.g.
 * {@code SYNC_UPDATES_AVAILABLE}) it triggers a cursor-based sync for the affected Item.
 *
 * <p>Note: Plaid signs webhooks with a JWT in the {@code Plaid-Verification} header. In a
 * production build you would verify that signature here before acting; it is intentionally
 * omitted in this sandbox portfolio build and called out as a known gap.
 */
@RestController
class PlaidWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PlaidWebhookController.class);

    private final TransactionSyncService syncService;

    PlaidWebhookController(TransactionSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/webhooks/plaid")
    ResponseEntity<Void> handle(@RequestBody PlaidWebhook webhook) {
        log.info("Plaid webhook received: type={} code={} item={}",
                webhook.webhookType(), webhook.webhookCode(), webhook.itemId());

        if ("TRANSACTIONS".equalsIgnoreCase(webhook.webhookType()) && webhook.itemId() != null) {
            syncService.syncByPlaidItemId(webhook.itemId());
        }
        // Always ack — Plaid retries on non-2xx, and the sync is idempotent anyway.
        return ResponseEntity.ok().build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlaidWebhook(
            @JsonProperty("webhook_type") String webhookType,
            @JsonProperty("webhook_code") String webhookCode,
            @JsonProperty("item_id") String itemId) {
    }
}
