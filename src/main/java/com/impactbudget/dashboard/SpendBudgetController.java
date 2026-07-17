package com.impactbudget.dashboard;

import com.impactbudget.budget.BudgetStatus;
import com.impactbudget.budget.SpendBudgetService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Read and set the user's overall monthly spending budget. */
@RestController
@RequestMapping("/api/budget")
class SpendBudgetController {

    private static final String DEFAULT_USER = "demo-user";

    private final SpendBudgetService budgetService;

    SpendBudgetController(SpendBudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    BudgetStatus status(@RequestParam(required = false) String userId,
                        @RequestParam(required = false) String month) {
        return budgetService.status(user(userId), month(month));
    }

    @PutMapping
    BudgetStatus setLimit(@RequestBody SetBudgetRequest request) {
        String user = user(request.userId());
        budgetService.setLimit(user, request.monthlyLimit());
        return budgetService.status(user, month(request.month()));
    }

    private String user(String userId) {
        return StringUtils.hasText(userId) ? userId : DEFAULT_USER;
    }

    private String month(String month) {
        return StringUtils.hasText(month) ? month : YearMonth.now().toString();
    }

    record SetBudgetRequest(
            String userId,
            String month,
            @NotNull @Positive BigDecimal monthlyLimit) {
    }
}
