package com.banking.transaction_service.event;

import java.time.LocalDateTime;

public interface TransactionEvent {

    String eventType();

    Long transactionId();

    String reference();

    LocalDateTime date();
}
