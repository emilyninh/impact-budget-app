package com.impactbudget.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

/**
 * Application-wide error handling, returning RFC-7807 {@link ProblemDetail} responses.
 * Module-specific handlers (e.g. the Plaid one) still apply to their own exception types;
 * this covers validation, status-bearing exceptions (incl. auth), and the catch-all.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean-validation failures on @Valid request bodies → 400 with the field errors. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                detail.isBlank() ? "Validation failed" : detail);
        pd.setTitle("Validation failed");
        return pd;
    }

    /** Exceptions that already declare a status (auth failures, 404s, …). */
    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail onStatus(ResponseStatusException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
        pd.setTitle(HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase());
        return pd;
    }

    /** Anything unmapped → 500, without leaking internals to the client. */
    @ExceptionHandler(Exception.class)
    ProblemDetail onUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        pd.setTitle("Internal Server Error");
        return pd;
    }
}
