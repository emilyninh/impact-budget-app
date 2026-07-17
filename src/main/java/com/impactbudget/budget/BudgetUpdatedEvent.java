package com.impactbudget.budget;

/**
 * Published (as a Spring application event) whenever a user's monthly aggregate changes, so
 * the dashboard module can push a live update over SSE without the budget module depending on
 * it (dashboard -> budget is the allowed direction).
 */
public record BudgetUpdatedEvent(String userId, String yearMonth) {
}
