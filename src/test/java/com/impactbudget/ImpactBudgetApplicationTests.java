package com.impactbudget;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Smoke test: the full Spring context boots against real Postgres/Kafka/Redis containers,
 * Flyway migrations apply cleanly, and JPA schema validation passes. If any wiring or
 * migration is broken, this fails.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ImpactBudgetApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty — success means the context (and all migrations) loaded.
    }
}
