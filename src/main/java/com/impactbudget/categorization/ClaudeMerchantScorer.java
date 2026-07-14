package com.impactbudget.categorization;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Scores merchants with the Claude API. Selected when {@code categorization.scoring.provider=claude}.
 * Returns the neutral fallback when no API key is configured or a call fails.
 */
@Component
class ClaudeMerchantScorer implements MerchantScorer {

    private static final Logger log = LoggerFactory.getLogger(ClaudeMerchantScorer.class);

    private final AnthropicProperties props;
    private final LlmScoringSupport support;
    private final AnthropicClient client;   // null when no API key is configured

    ClaudeMerchantScorer(AnthropicProperties props, LlmScoringSupport support) {
        this.props = props;
        this.support = support;
        this.client = StringUtils.hasText(props.apiKey())
                ? AnthropicOkHttpClient.builder().apiKey(props.apiKey()).build()
                : null;
    }

    @Override
    public String providerName() {
        return "claude";
    }

    @Override
    public MerchantScoring score(String normalized, String rawMerchant) {
        if (client == null) {
            return support.neutral(rawMerchant);
        }
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(props.modelOrDefault())
                    .maxTokens(512L)
                    .addSystemMessage(support.systemPrompt())
                    .addUserMessage(support.userPrompt(normalized, rawMerchant))
                    .build();
            Message message = client.messages().create(params);
            return support.parse(extractText(message), rawMerchant);
        } catch (Exception e) {
            log.warn("Claude scoring failed for '{}' ({}); using fallback", rawMerchant, e.toString());
            return support.neutral(rawMerchant);
        }
    }

    private String extractText(Message message) {
        return message.content().stream()
                .map(ContentBlock::text)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(TextBlock::text)
                .reduce("", String::concat);
    }
}
