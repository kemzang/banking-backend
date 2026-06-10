package com.banking.transaction_service.event;

import com.banking.transaction_service.enums.TypeTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionCompletedEvent(
        String eventType,
        Long transactionId,
        String reference,
        TypeTransaction transactionType,
        Long compteSourceId,
        Long compteDestId,
        BigDecimal montant,
        BigDecimal commission,
        String devise,
        LocalDateTime date
) implements TransactionEvent {
}
