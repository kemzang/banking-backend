package com.banking.transaction_service.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompensationRequestedEvent(
        String eventType,
        Long transactionId,
        String reference,
        Long compteSourceId,
        BigDecimal montantTotalDebite,
        String devise,
        LocalDateTime date
) implements TransactionEvent {
}
