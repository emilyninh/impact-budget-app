package com.impactbudget.assistant;

import java.util.List;

/** The assistant's reply plus the names of the tools it consulted (for a "show your work" note). */
public record ChatResponse(String reply, List<String> toolsUsed) {
}
