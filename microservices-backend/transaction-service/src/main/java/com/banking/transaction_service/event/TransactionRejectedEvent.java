package com.banking.transaction_service.event;

import com.banking.transaction_service.enums.TypeTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRejectedEvent(
        String eventType,
        Long transactionId,
        String reference,
        TypeTransaction transactionType,
        Long compteSourceId,
        Long compteDestId,
        BigDecimal montant,
        String devise,
        String motif,
        LocalDateTime date
) implements TransactionEvent {
}
