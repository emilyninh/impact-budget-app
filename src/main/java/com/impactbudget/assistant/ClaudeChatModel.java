package com.impactbudget.assistant;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.core.type.TypeReference;
import com.impactbudget.categorization.AnthropicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Claude-backed chat model (Anthropic SDK), mirroring {@code ClaudeMerchantScorer}'s setup. Highest
 * quality tool-calling, but paid per token. Selected when {@code assistant.provider=claude} and an
 * API key is configured.
 */
@Component
class ClaudeChatModel implements ChatModel {

    private static final Logger log = LoggerFactory.getLogger(ClaudeChatModel.class);

    private final AnthropicProperties props;
    private final AssistantTools tools;
    private final AnthropicClient client;   // null when no API key is configured
    private final List<Tool> toolDefs;

    ClaudeChatModel(AnthropicProperties props, AssistantTools tools) {
        this.props = props;
        this.tools = tools;
        this.client = StringUtils.hasText(props.apiKey())
                ? AnthropicOkHttpClient.builder().apiKey(props.apiKey()).build()
                : null;
        this.toolDefs = tools.specs().stream().map(ClaudeChatModel::toTool).toList();
    }

    @Override
    public String provider() {
        return "claude";
    }

    @Override
    public boolean available() {
        return client != null;
    }

    @Override
    public ChatResponse converse(String userId, List<Turn> messages) {
        if (client == null) {
            return new ChatResponse(
                    "The assistant is unavailable right now — no API key is configured.", List.of());
        }
        List<MessageParam> conversation = new ArrayList<>();
        for (Turn t : messages) {
            MessageParam.Role role = "assistant".equalsIgnoreCase(t.role())
                    ? MessageParam.Role.ASSISTANT : MessageParam.Role.USER;
            conversation.add(MessageParam.builder().role(role).content(t.content()).build());
        }

        List<String> toolsUsed = new ArrayList<>();
        try {
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                MessageCreateParams.Builder pb = MessageCreateParams.builder()
                        .model(props.modelOrDefault())
                        .maxTokens(1024L)
                        .system(SYSTEM_PROMPT)
                        .messages(conversation);
                toolDefs.forEach(pb::addTool);

                Message response = client.messages().create(pb.build());
                List<ToolUseBlock> calls = response.content().stream()
                        .map(ContentBlock::toolUse)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList();

                if (calls.isEmpty()) {
                    return new ChatResponse(extractText(response), toolsUsed);
                }

                conversation.add(response.toParam());
                List<ContentBlockParam> results = new ArrayList<>();
                for (ToolUseBlock call : calls) {
                    toolsUsed.add(call.name());
                    String result = tools.execute(userId, call.name(), inputOf(call));
                    results.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                            .toolUseId(call.id())
                            .content(result)
                            .build()));
                }
                conversation.add(MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .contentOfBlockParams(results)
                        .build());
            }
            return new ChatResponse(
                    "I couldn't finish answering that — try asking something more specific.", toolsUsed);
        } catch (Exception e) {
            log.warn("Claude chat failed: {}", e.toString());
            boolean badKey = e.toString().contains("authentication_error")
                    || e.toString().contains("invalid x-api-key");
            String message = badKey
                    ? "The assistant's API key is missing or invalid — set ANTHROPIC_API_KEY and restart."
                    : "Sorry — I hit an error answering that. Please try again.";
            return new ChatResponse(message, toolsUsed);
        }
    }

    private Map<String, Object> inputOf(ToolUseBlock call) {
        try {
            Map<String, Object> parsed = call._input().convert(new TypeReference<Map<String, Object>>() {
            });
            return parsed != null ? parsed : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String extractText(Message message) {
        return message.content().stream()
                .map(ContentBlock::text)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(TextBlock::text)
                .reduce("", String::concat)
                .trim();
    }

    /** Translate a plain tool spec into an Anthropic tool definition (string-typed params). */
    private static Tool toTool(AssistantTools.Spec spec) {
        Tool.InputSchema.Properties.Builder properties = Tool.InputSchema.Properties.builder();
        List<String> required = new ArrayList<>();
        for (AssistantTools.Param p : spec.params()) {
            properties.putAdditionalProperty(p.name(),
                    JsonValue.from(Map.of("type", "string", "description", p.description())));
            if (p.required()) {
                required.add(p.name());
            }
        }
        Tool.InputSchema schema = Tool.InputSchema.builder()
                .type(JsonValue.from("object"))
                .properties(properties.build())
                .required(required)
                .build();
        return Tool.builder()
                .name(spec.name())
                .description(spec.description())
                .inputSchema(schema)
                .build();
    }
}
