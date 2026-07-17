package com.impactbudget.budget;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Manages a user's overall monthly spending limit and reports live status against it.
 *
 * <p>Spend comes from the existing Redis-cached monthly aggregate ({@link BudgetAggregateService}),
 * so this service adds only the limit and the pace projection on top. For the current month it
 * projects month-end spend from the daily burn rate so the UI can warn before the limit is hit;
 * for a past month the projection is just the actual total.
 */
@Service
public class SpendBudgetService {

    private final SpendBudgetRepository repository;
    private final BudgetAggregateService aggregateService;

    public SpendBudgetService(SpendBudgetRepository repository,
                              BudgetAggregateService aggregateService) {
        this.repository = repository;
        this.aggregateService = aggregateService;
    }

    /** Set (or replace) the user's monthly limit; one active row per user. */
    public SpendBudget setLimit(String userId, BigDecimal monthlyLimit) {
        SpendBudget budget = repository.findByUserId(userId).orElseGet(() -> {
            SpendBudget b = new SpendBudget();
            b.setId(UUID.randomUUID());
            b.setUserId(userId);
            return b;
        });
        budget.setMonthlyLimit(monthlyLimit.setScale(2, RoundingMode.HALF_UP));
        return repository.save(budget);
    }

    /** Live status of spend vs. the limit for the given month (defaults handled by caller). */
    public BudgetStatus status(String userId, String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        BigDecimal spent = aggregateService.getMonthly(userId, yearMonth).totalSpend();
        int daysInMonth = ym.lengthOfMonth();
        int daysElapsed = daysElapsed(ym, daysInMonth);
        BigDecimal limit = repository.findByUserId(userId)
                .map(SpendBudget::getMonthlyLimit)
                .orElse(null);
        return evaluate(userId, yearMonth, spent, limit, daysElapsed, daysInMonth);
    }

    /**
     * Pure status computation from resolved inputs — projection, remaining, and classification.
     * Package-private so it can be unit-tested without a clock or repositories.
     */
    BudgetStatus evaluate(String userId, String yearMonth, BigDecimal spent, BigDecimal limit,
                          int daysElapsed, int daysInMonth) {
        BigDecimal projected = project(spent, daysElapsed, daysInMonth);

        if (limit == null) {
            return new BudgetStatus(userId, yearMonth, null, spent, null, 0.0,
                    daysElapsed, daysInMonth, projected, BudgetStatus.Status.NO_BUDGET);
        }

        BigDecimal remaining = limit.subtract(spent);
        double pctUsed = limit.signum() > 0
                ? Math.round(spent.divide(limit, 4, RoundingMode.HALF_UP).doubleValue() * 1000.0) / 10.0
                : 0.0;
        BudgetStatus.Status status;
        if (spent.compareTo(limit) > 0) {
            status = BudgetStatus.Status.OVER;
        } else if (projected.compareTo(limit) > 0) {
            status = BudgetStatus.Status.AT_RISK;
        } else {
            status = BudgetStatus.Status.ON_TRACK;
        }

        return new BudgetStatus(userId, yearMonth, limit, spent, remaining, pctUsed,
                daysElapsed, daysInMonth, projected, status);
    }

    /** Days counted so far: the day-of-month for the current month, the full month for a past one. */
    private int daysElapsed(YearMonth ym, int daysInMonth) {
        YearMonth now = YearMonth.now();
        if (ym.isBefore(now)) {
            return daysInMonth;                 // past month is complete
        }
        if (ym.isAfter(now)) {
            return 0;                           // future month — nothing spent/elapsed yet
        }
        return LocalDate.now().getDayOfMonth();  // current month, so far
    }

    /** Extrapolate month-end spend from the daily pace; actual total for a complete/empty month. */
    private BigDecimal project(BigDecimal spent, int daysElapsed, int daysInMonth) {
        if (daysElapsed <= 0 || daysElapsed >= daysInMonth) {
            return spent.setScale(2, RoundingMode.HALF_UP);
        }
        return spent.multiply(BigDecimal.valueOf(daysInMonth))
                .divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP);
    }
}
