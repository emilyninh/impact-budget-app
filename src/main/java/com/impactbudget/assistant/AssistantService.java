package com.impactbudget.assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * A grounded, agentic assistant over the user's own spending data. Selects a {@link ChatModel}
 * provider (free local Ollama by default, or paid Claude) and delegates the tool-use conversation
 * to it. Figures are never invented — the model answers from the real numbers the tools return
 * ("show your work"). Stateless: the frontend sends the running message history each turn.
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final AssistantProperties props;
    private final List<ChatModel> models;

    public AssistantService(AssistantProperties props, List<ChatModel> models) {
        this.props = props;
        this.models = models;
    }

    /** Answer the latest user turn, given the running history, via the configured chat model. */
    public ChatResponse chat(String userId, List<Turn> messages) {
        ChatModel model = select();
        if (model == null) {
            return new ChatResponse(
                    "The assistant is unavailable — no chat model is configured.", List.of());
        }
        return model.converse(userId, messages);
    }

    /** Prefer the configured provider if it can serve; otherwise fall back to any available model. */
    private ChatModel select() {
        String want = props.providerOrDefault();
        ChatModel preferred = models.stream()
                .filter(m -> m.provider().equalsIgnoreCase(want) && m.available())
                .findFirst()
                .orElse(null);
        if (preferred != null) {
            return preferred;
        }
        ChatModel fallback = models.stream().filter(ChatModel::available).findFirst().orElse(null);
        if (fallback != null && !fallback.provider().equalsIgnoreCase(want)) {
            log.info("Assistant provider '{}' unavailable; falling back to '{}'", want, fallback.provider());
        }
        return fallback;
    }
}
