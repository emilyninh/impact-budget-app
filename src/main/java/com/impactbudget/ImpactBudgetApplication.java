package com.impactbudget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Impact Budget — a personal finance tracker that categorizes spending by <em>impact</em>
 * (local/independent vs. multinational, sustainable vs. not) rather than by category.
 *
 * <p>Built as a modular monolith: one deployable Spring Boot app with clear module
 * boundaries ({@code ingestion}, {@code categorization}, {@code budget}, {@code dashboard},
 * {@code common}) that communicate over Kafka. See {@code README.md} for the architecture.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ImpactBudgetApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImpactBudgetApplication.class, args);
    }
}
