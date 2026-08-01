package com.impactbudget.assistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Chat endpoint for the grounded assistant. Authenticated automatically (not in the security
 * whitelist), so the answer is always scoped to the JWT subject — never a client-supplied id.
 */
@RestController
@RequestMapping("/api/v1/assistant")
class AssistantController {

    private final AssistantService assistant;

    AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    /** The running conversation; the server is stateless and answers the latest user turn. */
    record ChatRequest(@NotEmpty List<Turn> messages) {
    }

    @PostMapping("/chat")
    ChatResponse chat(@AuthenticationPrincipal String userId,
                      @Valid @RequestBody ChatRequest request) {
        return assistant.chat(userId, request.messages());
    }
}
