package com.impactbudget.budget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

/**
 * Dev-only demo goals for {@code demo-user}, gated by {@code demo.seed-enabled=true}.
 * Idempotent (skips if the user already has goals), so the goal tracker shows live progress
 * alongside the demo transactions.
 */
@Component
@ConditionalOnProperty(name = "demo.seed-enabled", havingValue = "true")
class DemoGoalSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoGoalSeeder.class);
    private static final String DEMO_USER = "demo-user";

    private final GoalRepository goalRepository;

    DemoGoalSeeder(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!goalRepository.findByUserId(DEMO_USER).isEmpty()) {
            return;
        }
        LocalDate yearEnd = LocalDate.of(LocalDate.now().getYear(), Month.DECEMBER, 31);
        goalRepository.save(goal(Goal.Dimension.LOCAL, 18, 30, yearEnd));
        goalRepository.save(goal(Goal.Dimension.SUSTAINABLE, 40, 60, yearEnd));
        log.info("Demo seed: 2 goals created for {}", DEMO_USER);
    }

    private Goal goal(Goal.Dimension dimension, int baseline, int target, LocalDate targetDate) {
        Goal g = new Goal();
        g.setId(UUID.randomUUID());
        g.setUserId(DEMO_USER);
        g.setDimension(dimension);
        g.setBaselinePct(baseline);
        g.setTargetPct(target);
        g.setTargetDate(targetDate);
        return g;
    }
}
