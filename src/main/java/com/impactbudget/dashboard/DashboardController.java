package com.impactbudget.dashboard;

import com.impactbudget.budget.BudgetAggregate;
import com.impactbudget.budget.BudgetAggregateService;
import com.impactbudget.budget.ScoredTransactionView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/**
 * Read API backing the React dashboard: the current impact summary, the multi-month trend,
 * and the scored transaction list. All reads flow through the Redis-cached aggregate and are
 * scoped to the authenticated user (the JWT subject) — never a client-supplied id.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController {

    private final BudgetAggregateService aggregateService;

    DashboardController(BudgetAggregateService aggregateService) {
        this.aggregateService = aggregateService;
    }

    @GetMapping("/summary")
    BudgetAggregate summary(@AuthenticationPrincipal String userId,
                            @RequestParam(required = false) String month) {
        return aggregateService.getMonthly(userId, month(month));
    }

    @GetMapping("/trend")
    List<BudgetAggregate> trend(@AuthenticationPrincipal String userId,
                                @RequestParam(defaultValue = "6") int months) {
        return aggregateService.trend(userId, Math.max(1, Math.min(24, months)));
    }

    @GetMapping("/transactions")
    List<ScoredTransactionView> transactions(@AuthenticationPrincipal String userId,
                                             @RequestParam(required = false) String month) {
        return aggregateService.recentTransactions(userId, month(month));
    }

    private String month(String month) {
        return StringUtils.hasText(month) ? month : YearMonth.now().toString();
    }
}
