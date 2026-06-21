package com.banking.loan_service.client;

import java.math.BigDecimal;

public interface AccountClient {
    void credit(Long accountId, BigDecimal amount, String motif);
}
