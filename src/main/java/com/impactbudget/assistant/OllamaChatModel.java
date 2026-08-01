package com.impactbudget.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.impactbudget.categorization.OllamaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ollama-backed chat model — free, private (nothing leaves the machine), no API key. Uses Ollama's
 * {@code /api/chat} with OpenAI-style {@code tools}, so it needs a tool-capable local model (e.g.
 * {@code llama3.1}). Selected when {@code assistant.provider=ollama} (the default). Degrades to a
 * helpful message if the server is unreachable or the model can't use tools.
 */
@Component
class OllamaChatModel implements ChatModel {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatModel.class);

    private final OllamaProperties props;
    private final AssistantTools tools;
    private final ObjectMapper mapper;
    private final RestClient http;

    OllamaChatModel(OllamaProperties props, AssistantTools tools, ObjectMapper mapper) {
        this.props = props;
        this.tools = tools;
        this.mapper = mapper;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(60));   // local generation + tool loops can be slow
        this.http = RestClient.builder().baseUrl(props.baseUrlOrDefault()).requestFactory(factory).build();
    }

    @Override
    public String provider() {
        return "ollama";
    }

    @Override
    public boolean available() {
        // No cheap liveness probe; assume reachable and degrade gracefully on error in converse().
        return true;
    }

    @Override
    public ChatResponse converse(String userId, List<Turn> turns) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("system", SYSTEM_PROMPT));
        for (Turn t : turns) {
            messages.add(msg("assistant".equalsIgnoreCase(t.role()) ? "assistant" : "user", t.content()));
        }
        List<Map<String, Object>> toolDefs = toolDefs();
        List<String> toolsUsed = new ArrayList<>();

        try {
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                Map<String, Object> body = Map.of(
                        "model", props.modelOrDefault(),
                        "stream", false,
                        "messages", messages,
                        "tools", toolDefs);

                String response = http.post()
                        .uri("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                JsonNode message = mapper.readTree(response).path("message");
                JsonNode toolCalls = message.path("tool_calls");

                if (!toolCalls.isArray() || toolCalls.isEmpty()) {
                    return new ChatResponse(message.path("content").asText("").trim(), toolsUsed);
                }

                // Keep the assistant's tool-call turn in context, then answer each call.
                messages.add(mapper.convertValue(message, new com.fasterxml.jackson.core.type.TypeReference<>() {
                }));
                for (JsonNode call : toolCalls) {
                    String name = call.path("function").path("name").asText();
                    toolsUsed.add(name);
                    String result = tools.execute(userId, name, argsOf(call.path("function").path("arguments")));
                    Map<String, Object> toolMsg = new LinkedHashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("content", result);
                    toolMsg.put("tool_name", name);   // newer Ollama associates results by name
                    messages.add(toolMsg);
                }
            }
            return new ChatResponse(
                    "I couldn't finish answering that — try asking something more specific.", toolsUsed);
        } catch (Exception e) {
            log.warn("Ollama chat failed ({}); is `ollama serve` running with a tool-capable model '{}'?",
                    e.toString(), props.modelOrDefault());
            return new ChatResponse(
                    "The local assistant isn't reachable — start `ollama serve` with a tool-capable model "
                            + "(e.g. `ollama run " + props.modelOrDefault() + "`) and try again.", toolsUsed);
        }
    }

    /** Ollama returns tool arguments as a JSON object (or, on some versions, a JSON string). */
    private Map<String, Object> argsOf(JsonNode arguments) {
        try {
            JsonNode node = arguments.isTextual() ? mapper.readTree(arguments.asText()) : arguments;
            if (node == null || node.isMissingNode() || !node.isObject()) {
                return Map.of();
            }
            return mapper.convertValue(node, new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static Map<String, Object> msg(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    /** OpenAI-style function tool schemas from the plain specs. */
    private List<Map<String, Object>> toolDefs() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (AssistantTools.Spec spec : tools.specs()) {
            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();
            for (AssistantTools.Param p : spec.params()) {
                properties.put(p.name(), Map.of("type", "string", "description", p.description()));
                if (p.required()) {
                    required.add(p.name());
                }
            }
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("type", "object");
            parameters.put("properties", properties);
            parameters.put("required", required);

            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", spec.name());
            function.put("description", spec.description());
            function.put("parameters", parameters);

            out.add(Map.of("type", "function", "function", function));
        }
        return out;
    }
}
