package com.banking.transaction_service.exception;

public class DeviseIncompatibleException extends RuntimeException {

    public DeviseIncompatibleException(String message) {
        super(message);
    }
}
