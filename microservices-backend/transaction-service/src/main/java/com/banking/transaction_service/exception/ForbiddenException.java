package com.banking.transaction_service.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends RuntimeException {
    private final HttpStatus status = HttpStatus.FORBIDDEN;

    public ForbiddenException(String message) {
        super(message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}