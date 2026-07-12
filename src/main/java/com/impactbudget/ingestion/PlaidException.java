package com.impactbudget.ingestion;

/** Raised when a Plaid API call fails or returns a non-successful response. */
public class PlaidException extends RuntimeException {

    public PlaidException(String message) {
        super(message);
    }

    public PlaidException(String message, Throwable cause) {
        super(message, cause);
    }
}
