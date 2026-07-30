package com.impactbudget.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Plaid credentials and environment, bound from the {@code plaid.*} config keys.
 */
@ConfigurationProperties(prefix = "plaid")
public record PlaidProperties(
        String clientId,
        String secret,
        String environment,   // sandbox | production
        String webhookUrl,
        String redirectUri    // OAuth return URL; must exactly match a Dashboard-registered URI
) {
}
