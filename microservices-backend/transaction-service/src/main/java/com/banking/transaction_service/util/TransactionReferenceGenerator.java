package com.banking.transaction_service.util;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Year;

@Component
public class TransactionReferenceGenerator {

    private final Clock clock;

    public TransactionReferenceGenerator() {
        this(Clock.systemDefaultZone());
    }

    TransactionReferenceGenerator(Clock clock) {
        this.clock = clock;
    }

    public String generate(Long transactionId) {
        int year = Year.now(clock).getValue();
        return "TX-%d-%06d".formatted(year, transactionId);
    }
}
