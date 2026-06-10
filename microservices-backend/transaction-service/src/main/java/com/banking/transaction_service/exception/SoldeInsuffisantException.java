package com.banking.transaction_service.exception;

public class SoldeInsuffisantException extends RuntimeException {

    public SoldeInsuffisantException(String message) {
        super(message);
    }
}
