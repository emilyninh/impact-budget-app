package com.impactbudget;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifies the modular-monolith boundaries statically: no cyclic dependencies between
 * modules and no access to another module's internals. Fails the build if the architecture
 * drifts (e.g. ingestion starts reaching into budget's tables).
 */
class ModularityTest {

    @Test
    void moduleBoundariesAreRespected() {
        ApplicationModules.of(ImpactBudgetApplication.class).verify();
    }
}
