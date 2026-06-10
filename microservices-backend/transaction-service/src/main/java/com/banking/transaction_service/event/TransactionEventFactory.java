package com.banking.transaction_service.event;

import com.banking.transaction_service.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

final class TransactionEventFactory {

    private TransactionEventFactory() {
    }

    static TransactionCompletedEvent completed(Transaction transaction) {
        return new TransactionCompletedEvent(
                "transaction.completed",
                transaction.getId(),
                transaction.getReference(),
                transaction.getType(),
                transaction.getCompteSourceId(),
                transaction.getCompteDestId(),
                transaction.getMontant(),
                transaction.getCommission(),
                transaction.getDevise(),
                eventDate(transaction)
        );
    }

    static TransactionRejectedEvent rejected(Transaction transaction) {
        return new TransactionRejectedEvent(
                "transaction.rejected",
                transaction.getId(),
                transaction.getReference(),
                transaction.getType(),
                transaction.getCompteSourceId(),
                transaction.getCompteDestId(),
                transaction.getMontant(),
                transaction.getDevise(),
                transaction.getMotif(),
                eventDate(transaction)
        );
    }

    static CompensationRequestedEvent compensation(
            Transaction transaction,
            Long compteId,
            BigDecimal montant
    ) {
        return new CompensationRequestedEvent(
                "transaction.compensation.requested",
                transaction.getId(),
                transaction.getReference(),
                compteId,
                montant,
                transaction.getDevise(),
                LocalDateTime.now()
        );
    }

    private static LocalDateTime eventDate(Transaction transaction) {
        return transaction.getDateOperation() != null
                ? transaction.getDateOperation()
                : LocalDateTime.now();
    }
}
