package com.impactbudget.auth;

/** The result of a successful register/login: a bearer token plus a small user summary. */
public record AuthResult(
        String token,
        long expiresInSeconds,
        String userId,
        String email,
        String displayName) {
}
