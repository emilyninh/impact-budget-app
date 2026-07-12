package com.impactbudget.categorization;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Calls Claude to assess a merchant's local and sustainability impact, returning strict
 * JSON that is parsed with Jackson. If no API key is configured — or the call/parse fails —
 * it returns a neutral fallback so the pipeline keeps working (and the app runs keyless).
 */
@Component
public class MerchantScoringClient {

    private static final Logger log = LoggerFactory.getLogger(MerchantScoringClient.class);

    private static final String SYSTEM_PROMPT = """
            You are a merchant classifier for a values-based budgeting app. Given a bank
            merchant descriptor, identify the real merchant and assess two dimensions.

            Respond with ONLY a JSON object (no prose, no markdown fences) matching exactly:
            {
              "cleanedMerchant": string,
              "category": string,
              "localScore": integer 0-100,          // 0 = multinational conglomerate, 100 = local independently-owned
              "localIndependent": boolean,
              "sustainabilityScore": integer 0-100,  // 0 = high-footprint / fast fashion / synthetic, 100 = B-Corp / organic / natural fiber / plant-based
              "materialFlags": [string],             // e.g. ["polyester","organic","merino-wool"]
              "confidence": number 0-1,
              "rationale": string                    // one short sentence
            }
            If unsure, use moderate scores and low confidence.
            """;

    private final AnthropicProperties props;
    private final ObjectMapper mapper;
    private final MeterRegistry meterRegistry;
    private final AnthropicClient client;   // null when no API key is configured

    public MerchantScoringClient(AnthropicProperties props, ObjectMapper mapper, MeterRegistry meterRegistry) {
        this.props = props;
        this.mapper = mapper;
        this.meterRegistry = meterRegistry;
        if (StringUtils.hasText(props.apiKey())) {
            this.client = AnthropicOkHttpClient.builder().apiKey(props.apiKey()).build();
            log.info("Anthropic client configured (model={})", props.modelOrDefault());
        } else {
            this.client = null;
            log.warn("No ANTHROPIC_API_KEY set — categorization will use the neutral fallback heuristic");
        }
    }

    public MerchantScoring score(String normalized, String rawMerchant) {
        if (client == null) {
            meterRegistry.counter("categorization.scoring.total", "source", "fallback").increment();
            return fallback(rawMerchant);
        }
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(props.modelOrDefault())
                    .maxTokens(512L)
                    .addSystemMessage(SYSTEM_PROMPT)
                    .addUserMessage(userPrompt(normalized, rawMerchant))
                    .build();

            Message message = client.messages().create(params);
            MerchantScoring result = parse(extractText(message), rawMerchant);
            meterRegistry.counter("categorization.scoring.total", "source", "llm").increment();
            return result;
        } catch (Exception e) {
            meterRegistry.counter("categorization.scoring.total", "source", "fallback").increment();
            log.warn("Claude scoring failed for '{}' ({}); using fallback", rawMerchant, e.toString());
            return fallback(rawMerchant);
        } finally {
            // Latency of the Claude round-trip (visible in Grafana as categorization_claude_latency_*).
            sample.stop(meterRegistry.timer("categorization.claude.latency"));
        }
    }

    private String userPrompt(String normalized, String rawMerchant) {
        return "Merchant descriptor: \"" + rawMerchant + "\"\n"
                + "Normalized: \"" + normalized + "\"\n"
                + "Return the JSON now.";
    }

    private String extractText(Message message) {
        return message.content().stream()
                .map(ContentBlock::text)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(TextBlock::text)
                .reduce("", String::concat);
    }

    private MerchantScoring parse(String text, String rawMerchant) throws Exception {
        String json = text;
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        LlmJson j = mapper.readValue(json, LlmJson.class);

        int local = clamp(j.localScore() != null ? j.localScore() : 40);
        int sustainability = clamp(j.sustainabilityScore() != null ? j.sustainabilityScore() : 50);
        boolean independent = Boolean.TRUE.equals(j.localIndependent());
        List<String> flags = j.materialFlags() != null ? j.materialFlags() : List.of();
        double confidence = j.confidence() != null ? j.confidence() : 0.5;
        String cleaned = StringUtils.hasText(j.cleanedMerchant()) ? j.cleanedMerchant() : rawMerchant;

        return new MerchantScoring(cleaned, j.category(), local, independent, sustainability,
                flags, confidence, j.rationale(), MerchantScoring.SOURCE_LLM);
    }

    private MerchantScoring fallback(String rawMerchant) {
        return new MerchantScoring(rawMerchant, null, 40, false, 50, List.of(), 0.2,
                "Neutral heuristic (no API key or scoring unavailable).", MerchantScoring.SOURCE_FALLBACK);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    /** Shape of the JSON Claude returns; extra/missing fields tolerated. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record LlmJson(
            String cleanedMerchant,
            String category,
            Integer localScore,
            Boolean localIndependent,
            Integer sustainabilityScore,
            List<String> materialFlags,
            Double confidence,
            String rationale) {
    }
}
