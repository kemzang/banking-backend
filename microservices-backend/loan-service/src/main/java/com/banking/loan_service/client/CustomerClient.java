package com.banking.loan_service.client;

import com.banking.loan_service.dto.CustomerResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerClient {
    @GetMapping("/api/customers/{id}")
    CustomerResponseDTO getById(@PathVariable Long id);

    @GetMapping("/api/customers/by-email/{email}")
    CustomerResponseDTO getByEmail(@PathVariable String email);
}
