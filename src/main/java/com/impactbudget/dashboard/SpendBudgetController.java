package com.impactbudget.dashboard;

import com.impactbudget.budget.BudgetStatus;
import com.impactbudget.budget.SpendBudgetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Read and set the authenticated user's overall monthly spending budget. */
@RestController
@RequestMapping("/api/v1/budget")
class SpendBudgetController {

    private final SpendBudgetService budgetService;

    SpendBudgetController(SpendBudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    BudgetStatus status(@AuthenticationPrincipal String userId,
                        @RequestParam(required = false) String month) {
        return budgetService.status(userId, month(month));
    }

    @PutMapping
    BudgetStatus setLimit(@AuthenticationPrincipal String userId, @Valid @RequestBody SetBudgetRequest request) {
        budgetService.setLimit(userId, request.monthlyLimit());
        return budgetService.status(userId, month(request.month()));
    }

    private String month(String month) {
        return StringUtils.hasText(month) ? month : YearMonth.now().toString();
    }

    record SetBudgetRequest(
            String month,
            @NotNull @Positive BigDecimal monthlyLimit) {
    }
}
