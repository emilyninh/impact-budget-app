package com.impactbudget.dashboard;

import com.impactbudget.budget.Goal;
import com.impactbudget.budget.GoalProgress;
import com.impactbudget.budget.GoalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Create goals and read their live progress, scoped to the authenticated user. */
@RestController
@RequestMapping("/api/v1/goals")
class GoalController {

    private final GoalService goalService;

    GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    List<GoalProgress> progress(@AuthenticationPrincipal String userId) {
        return goalService.progress(userId);
    }

    @PostMapping
    Goal create(@AuthenticationPrincipal String userId, @Valid @RequestBody CreateGoalRequest request) {
        return goalService.createGoal(
                userId,
                request.dimension(),
                request.baselinePct(),
                request.targetPct(),
                request.targetDate());
    }

    record CreateGoalRequest(
            @NotNull Goal.Dimension dimension,
            int baselinePct,
            int targetPct,
            @NotNull LocalDate targetDate) {
    }
}
