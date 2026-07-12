package com.impactbudget.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps Plaid integration failures to RFC 7807 problem responses (HTTP 502). */
@RestControllerAdvice
class PlaidExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PlaidExceptionHandler.class);

    @ExceptionHandler(PlaidException.class)
    ProblemDetail handlePlaid(PlaidException ex) {
        log.warn("Plaid integration error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problem.setTitle("Plaid integration error");
        return problem;
    }
}
