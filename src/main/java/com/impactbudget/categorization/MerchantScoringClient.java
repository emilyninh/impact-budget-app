package com.impactbudget.categorization;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Facade over the configured {@link MerchantScorer} ({@code ollama} | {@code claude} |
 * {@code none}). Records scoring latency and source metrics, and guarantees a result even
 * if no provider is selected.
 */
@Component
public class MerchantScoringClient {

    private static final Logger log = LoggerFactory.getLogger(MerchantScoringClient.class);

    private final MerchantScorer active;   // null → neutral only
    private final LlmScoringSupport support;
    private final MeterRegistry meterRegistry;
    private final String providerName;

    public MerchantScoringClient(List<MerchantScorer> scorers,
                                 ScoringProperties props,
                                 LlmScoringSupport support,
                                 MeterRegistry meterRegistry) {
        this.support = support;
        this.meterRegistry = meterRegistry;
        this.active = scorers.stream()
                .filter(s -> s.providerName().equalsIgnoreCase(props.providerOrDefault()))
                .findFirst()
                .orElse(null);
        this.providerName = active != null ? active.providerName() : "none";
        log.info("Merchant scoring provider: {}", providerName);
    }

    public MerchantScoring score(String normalized, String rawMerchant) {
        if (active == null) {
            meterRegistry.counter("categorization.scoring.total", "source", "fallback").increment();
            return support.neutral(rawMerchant);
        }
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            MerchantScoring result = active.score(normalized, rawMerchant);
            meterRegistry.counter("categorization.scoring.total",
                    "source", result.source().toLowerCase(), "provider", providerName).increment();
            return result;
        } finally {
            sample.stop(meterRegistry.timer("categorization.scoring.latency", "provider", providerName));
        }
    }
}
