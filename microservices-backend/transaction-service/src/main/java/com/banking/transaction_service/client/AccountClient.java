package com.banking.transaction_service.client;

import com.banking.transaction_service.dto.AccountResponseDTO;

import java.math.BigDecimal;

public interface AccountClient {

    AccountResponseDTO getById(Long accountId);

    void credit(Long accountId, BigDecimal amount);

    void debit(Long accountId, BigDecimal amount);
}
