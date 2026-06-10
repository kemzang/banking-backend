package com.banking.transaction_service.event;

import com.banking.transaction_service.entity.Transaction;

import java.math.BigDecimal;

public interface TransactionEventPublisher {

    void publishTransactionCompleted(Transaction transaction);

    void publishTransactionRejected(Transaction transaction);

    void publishCompensationRequested(
            Transaction transaction,
            Long compteId,
            BigDecimal montant
    );
}
