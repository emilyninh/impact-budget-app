package com.impactbudget;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: the full Spring context boots against real Postgres/Kafka/Redis containers,
 * Flyway migrations apply cleanly, and JPA schema validation passes. If any wiring or
 * migration is broken, this fails.
 *
 * <p>{@code disabledWithoutDocker = true} skips this cleanly on machines with no Docker
 * daemon (so {@code mvn verify} still passes locally) while it runs in CI, which has Docker.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ImpactBudgetApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty — success means the context (and all migrations) loaded.
    }
}
