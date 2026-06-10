package com.banking.transaction_service.exception;

import org.springframework.http.HttpStatusCode;

public class AccountServiceException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public AccountServiceException(
            HttpStatusCode statusCode,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
