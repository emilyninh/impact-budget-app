package com.impactbudget.budget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    GoalRepository goalRepository;
    @Mock
    BudgetAggregateService aggregateService;

    @InjectMocks
    GoalService service;

    private Goal localGoal() {
        Goal g = new Goal();
        g.setId(UUID.randomUUID());
        g.setUserId("user-1");
        g.setDimension(Goal.Dimension.LOCAL);
        g.setBaselinePct(18);
        g.setTargetPct(30);
        g.setTargetDate(LocalDate.of(2026, 12, 31));
        return g;
    }

    @Test
    void progressIsMeasuredFromBaselineTowardTarget() {
        Goal goal = localGoal();
        when(goalRepository.findByUserId("user-1")).thenReturn(List.of(goal));
        when(aggregateService.getMonthly(anyString(), anyString())).thenReturn(new BudgetAggregate(
                "user-1", "2026-07", new BigDecimal("500.00"),
                24.0,   // current local impact %
                55.0, new BigDecimal("120.00"), 10, 100.0));

        List<GoalProgress> progress = service.progress("user-1");

        assertThat(progress).hasSize(1);
        GoalProgress p = progress.get(0);
        assertThat(p.currentPct()).isEqualTo(24.0);
        // (24 - 18) / (30 - 18) * 100 = 50.0
        assertThat(p.progressPct()).isEqualTo(50.0);
        assertThat(p.achieved()).isFalse();
    }

    @Test
    void reachingTargetMarksGoalAchieved() {
        Goal goal = localGoal();
        when(goalRepository.findByUserId("user-1")).thenReturn(List.of(goal));
        when(aggregateService.getMonthly(anyString(), anyString())).thenReturn(new BudgetAggregate(
                "user-1", "2026-07", new BigDecimal("500.00"),
                32.0, 55.0, new BigDecimal("160.00"), 10, 100.0));

        GoalProgress p = service.progress("user-1").get(0);

        assertThat(p.progressPct()).isEqualTo(100.0);
        assertThat(p.achieved()).isTrue();
    }
}
