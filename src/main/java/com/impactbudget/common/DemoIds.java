package com.impactbudget.common;

/**
 * Fixed identifiers for the seeded demo account, shared across the demo seeders so the
 * sample transactions, goals, and budget all attach to the same logged-in demo user.
 */
public final class DemoIds {

    private DemoIds() {
    }

    /** Stable UUID (as text) used as the {@code userId} on all seeded demo data. */
    public static final String DEMO_USER_ID = "11111111-1111-1111-1111-111111111111";

    public static final String DEMO_EMAIL = "demo@impactbudget.app";

    /** Demo password — documented in the README so reviewers can log in. */
    public static final String DEMO_PASSWORD = "demopass123";
}
