package com.banking.transaction_service.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(Long id) {
        super("Transaction introuvable avec l'identifiant " + id);
    }
}
