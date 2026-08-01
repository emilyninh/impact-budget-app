package com.impactbudget.assistant;

import java.util.List;

/**
 * A back end that can hold a grounded, tool-using conversation over the user's spending data.
 * Implementations wrap a specific LLM provider (Claude, Ollama) but share the same tool layer
 * ({@link AssistantTools}) and system contract below.
 */
public interface ChatModel {

    /** Safety cap on tool-use round trips for a single question. */
    int MAX_ITERATIONS = 6;

    /** Shared behavioural contract — grounded, and reporting rather than coaching (DESIGN.md voice). */
    String SYSTEM_PROMPT = """
            You are the assistant inside Impact Budget, a personal-finance app that scores spending on
            two axes: local (how independent/local the merchant is) and sustainability. Answer the
            user's questions about their spending, budget, and the impact of their purchases.

            Rules:
            - Only state figures you obtained from a tool. Never estimate or invent numbers. If a tool
              returns no data, say so plainly.
            - Copy dollar amounts and percentages from the tool output EXACTLY, digit for digit (e.g.
              128.55 stays 128.55). Never round, truncate, or recompute them.
            - Prefer calling a tool over guessing. Use get_budget_status for over/under budget,
              get_month_summary for impact percentages, get_category_spend for category totals,
              score_store to assess a specific store, get_greener_alternatives for swap ideas.
            - Report; do not coach. State what the data says and stop. No praise, no scolding, no
              encouragement, no exclamation points. This is the user's real money.
            - Be concise — a sentence or two. Use plain dollar amounts and whole/one-decimal percents.
            - When you cite a store's sustainability, mention the credential behind it if the tool
              provides one (e.g. GOTS organic, B Corp).
            """;

    /** The provider name this model answers to (matched against {@code assistant.provider}). */
    String provider();

    /** Whether this model can currently serve a request (e.g. an API key is configured). */
    boolean available();

    /** Run the grounded tool-use loop and return the final answer. Never throws. */
    ChatResponse converse(String userId, List<Turn> messages);
}
