package com.banking.loan_service.client;

import com.banking.loan_service.dto.AccountResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "account-service")
public interface AccountClient {
    @GetMapping("/api/accounts/{id}")
    AccountResponseDTO getById(@PathVariable("id") Long id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Roles") String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId);
}
