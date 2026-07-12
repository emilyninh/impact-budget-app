package com.impactbudget.dashboard;

import com.impactbudget.budget.Goal;
import com.impactbudget.budget.GoalProgress;
import com.impactbudget.budget.GoalService;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Create goals and read their live progress. */
@RestController
@RequestMapping("/api/goals")
class GoalController {

    private static final String DEFAULT_USER = "demo-user";

    private final GoalService goalService;

    GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    List<GoalProgress> progress(@RequestParam(required = false) String userId) {
        return goalService.progress(user(userId));
    }

    @PostMapping
    Goal create(@RequestBody CreateGoalRequest request) {
        return goalService.createGoal(
                user(request.userId()),
                request.dimension(),
                request.baselinePct(),
                request.targetPct(),
                request.targetDate());
    }

    private String user(String userId) {
        return StringUtils.hasText(userId) ? userId : DEFAULT_USER;
    }

    record CreateGoalRequest(
            String userId,
            @NotNull Goal.Dimension dimension,
            int baselinePct,
            int targetPct,
            @NotNull LocalDate targetDate) {
    }
}
