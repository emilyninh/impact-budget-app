package com.impactbudget.budget;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Manages spending-shift goals and computes live progress against the current month's
 * impact aggregate.
 */
@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final BudgetAggregateService aggregateService;

    public GoalService(GoalRepository goalRepository, BudgetAggregateService aggregateService) {
        this.goalRepository = goalRepository;
        this.aggregateService = aggregateService;
    }

    public Goal createGoal(String userId, Goal.Dimension dimension,
                           int baselinePct, int targetPct, LocalDate targetDate) {
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setUserId(userId);
        goal.setDimension(dimension);
        goal.setBaselinePct(baselinePct);
        goal.setTargetPct(targetPct);
        goal.setTargetDate(targetDate);
        return goalRepository.save(goal);
    }

    public List<Goal> listGoals(String userId) {
        return goalRepository.findByUserId(userId);
    }

    /** Progress for every goal, evaluated against the current month's aggregate. */
    public List<GoalProgress> progress(String userId) {
        String currentMonth = YearMonth.now().toString();
        BudgetAggregate aggregate = aggregateService.getMonthly(userId, currentMonth);
        return goalRepository.findByUserId(userId).stream()
                .map(goal -> toProgress(goal, aggregate))
                .toList();
    }

    private GoalProgress toProgress(Goal goal, BudgetAggregate aggregate) {
        double current = goal.getDimension() == Goal.Dimension.LOCAL
                ? aggregate.localImpactPct()
                : aggregate.sustainabilityImpactPct();

        double progressPct = computeProgress(goal.getBaselinePct(), goal.getTargetPct(), current);
        boolean achieved = current >= goal.getTargetPct();

        return new GoalProgress(
                goal.getId(), goal.getDimension(), goal.getBaselinePct(), goal.getTargetPct(),
                current, goal.getTargetDate(), progressPct, achieved);
    }

    private double computeProgress(int baseline, int target, double current) {
        int span = target - baseline;
        if (span == 0) {
            return current >= target ? 100.0 : 0.0;
        }
        double raw = (current - baseline) / (double) span * 100.0;
        double clamped = Math.max(0.0, Math.min(100.0, raw));
        return Math.round(clamped * 10.0) / 10.0;
    }
}
