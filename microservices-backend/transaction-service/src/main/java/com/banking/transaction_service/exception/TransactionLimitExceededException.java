package com.banking.transaction_service.exception;

import java.math.BigDecimal;

public class TransactionLimitExceededException extends RuntimeException {

    public TransactionLimitExceededException(BigDecimal maxAmount) {
        super("Le montant depasse le plafond autorise de " + maxAmount);
    }
}
