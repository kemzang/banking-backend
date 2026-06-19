package com.banking.transaction_service.client;

import com.banking.transaction_service.dto.ClientResponseDTO;

public interface CustomerClient {

    ClientResponseDTO getClientByEmail(String email);
}
