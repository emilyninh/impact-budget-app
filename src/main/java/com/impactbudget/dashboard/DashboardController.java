package com.impactbudget.dashboard;

import com.impactbudget.budget.BudgetAggregate;
import com.impactbudget.budget.BudgetAggregateService;
import com.impactbudget.budget.ScoredTransactionView;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/**
 * Read API backing the React dashboard: the current impact summary, the multi-month trend,
 * and the scored transaction list. All reads flow through the Redis-cached aggregate.
 */
@RestController
@RequestMapping("/api/dashboard")
class DashboardController {

    private static final String DEFAULT_USER = "demo-user";

    private final BudgetAggregateService aggregateService;

    DashboardController(BudgetAggregateService aggregateService) {
        this.aggregateService = aggregateService;
    }

    @GetMapping("/summary")
    BudgetAggregate summary(@RequestParam(required = false) String userId,
                            @RequestParam(required = false) String month) {
        return aggregateService.getMonthly(user(userId), month(month));
    }

    @GetMapping("/trend")
    List<BudgetAggregate> trend(@RequestParam(required = false) String userId,
                                @RequestParam(defaultValue = "6") int months) {
        return aggregateService.trend(user(userId), Math.max(1, Math.min(24, months)));
    }

    @GetMapping("/transactions")
    List<ScoredTransactionView> transactions(@RequestParam(required = false) String userId,
                                             @RequestParam(required = false) String month) {
        return aggregateService.recentTransactions(user(userId), month(month));
    }

    private String user(String userId) {
        return StringUtils.hasText(userId) ? userId : DEFAULT_USER;
    }

    private String month(String month) {
        return StringUtils.hasText(month) ? month : YearMonth.now().toString();
    }
}
