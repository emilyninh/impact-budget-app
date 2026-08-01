package com.impactbudget.assistant;

/** One conversation turn from the client: {@code role} is "user" or "assistant". */
public record Turn(String role, String content) {
}
