package com.impactbudget.dashboard;

import com.impactbudget.budget.BudgetAggregate;
import com.impactbudget.budget.BudgetAggregateService;
import com.impactbudget.budget.BudgetUpdatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges the budget module's {@link BudgetUpdatedEvent} to the SSE hub: on each change it
 * computes the user's fresh summary and pushes it live to their open dashboard streams.
 */
@Component
class DashboardUpdateNotifier {

    private final BudgetAggregateService aggregateService;
    private final SseHub hub;

    DashboardUpdateNotifier(BudgetAggregateService aggregateService, SseHub hub) {
        this.aggregateService = aggregateService;
        this.hub = hub;
    }

    @EventListener
    void onBudgetUpdated(BudgetUpdatedEvent event) {
        BudgetAggregate summary = aggregateService.getMonthly(event.userId(), event.yearMonth());
        hub.push(event.userId(), "dashboard-update", summary);
    }
}
