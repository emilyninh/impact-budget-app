package com.impactbudget.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Auth failures. They extend {@link ResponseStatusException} so the generic global handler
 * maps them to the right HTTP status without importing this module.
 */
public final class AuthExceptions {

    private AuthExceptions() {
    }

    /** Registration with an email that already exists → 409. */
    public static class EmailAlreadyUsedException extends ResponseStatusException {
        public EmailAlreadyUsedException(String email) {
            super(HttpStatus.CONFLICT, "Email already registered: " + email);
        }
    }

    /** Bad email/password on login → 401. */
    public static class InvalidCredentialsException extends ResponseStatusException {
        public InvalidCredentialsException() {
            super(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }
}
