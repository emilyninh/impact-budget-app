package com.impactbudget.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT signing config. The secret must be at least 256 bits (32+ chars) for HS256; in
 * production it is supplied via the {@code JWT_SECRET} env var.
 */
@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(String secret, Duration ttl) {

    public JwtProperties {
        if (ttl == null) {
            ttl = Duration.ofHours(12);
        }
    }
}
