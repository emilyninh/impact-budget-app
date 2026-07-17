package com.impactbudget.budget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

/**
 * Dev-only demo goals and a monthly budget for {@code demo-user}, gated by
 * {@code demo.seed-enabled=true}. Idempotent (skips each item the user already has), so the
 * goal tracker and budget tracker show live progress alongside the demo transactions.
 */
@Component
@ConditionalOnProperty(name = "demo.seed-enabled", havingValue = "true")
class DemoGoalSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoGoalSeeder.class);
    private static final String DEMO_USER = "demo-user";

    private final GoalRepository goalRepository;
    private final SpendBudgetService spendBudgetService;
    private final SpendBudgetRepository spendBudgetRepository;

    DemoGoalSeeder(GoalRepository goalRepository,
                   SpendBudgetService spendBudgetService,
                   SpendBudgetRepository spendBudgetRepository) {
        this.goalRepository = goalRepository;
        this.spendBudgetService = spendBudgetService;
        this.spendBudgetRepository = spendBudgetRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedGoals();
        seedBudget();
    }

    private void seedGoals() {
        if (!goalRepository.findByUserId(DEMO_USER).isEmpty()) {
            return;
        }
        LocalDate yearEnd = LocalDate.of(LocalDate.now().getYear(), Month.DECEMBER, 31);
        goalRepository.save(goal(Goal.Dimension.LOCAL, 18, 30, yearEnd));
        goalRepository.save(goal(Goal.Dimension.SUSTAINABLE, 40, 60, yearEnd));
        log.info("Demo seed: 2 goals created for {}", DEMO_USER);
    }

    private void seedBudget() {
        if (spendBudgetRepository.findByUserId(DEMO_USER).isPresent()) {
            return;
        }
        // ~$500/month — demo spend is ~$411, so the tracker shows a meaningful ~82% bar.
        spendBudgetService.setLimit(DEMO_USER, new BigDecimal("500.00"));
        log.info("Demo seed: $500/month budget created for {}", DEMO_USER);
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
