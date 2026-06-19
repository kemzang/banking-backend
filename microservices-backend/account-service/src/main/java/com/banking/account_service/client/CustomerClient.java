package com.banking.account_service.client;

import com.banking.account_service.dto.ClientResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerClient {

    @GetMapping("/api/customers/by-email/{email}")
    ClientResponseDTO getClientByEmail(@PathVariable("email") String email);
}
