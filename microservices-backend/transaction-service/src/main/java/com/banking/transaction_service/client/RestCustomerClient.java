package com.banking.transaction_service.client;

import com.banking.transaction_service.dto.ClientResponseDTO;
import com.banking.transaction_service.exception.InvalidTransactionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestCustomerClient implements CustomerClient {

    private final RestClient customerRestClient;

    public RestCustomerClient(RestClient customerRestClient) {
        this.customerRestClient = customerRestClient;
    }

    @Override
    public ClientResponseDTO getClientByEmail(String email) {
        try {
            return customerRestClient.get()
                    .uri("/api/customers/by-email/{email}", email)
                    .retrieve()
                    .body(ClientResponseDTO.class);
        } catch (RestClientResponseException e) {
            throw new InvalidTransactionException("Client non trouvé pour cet email");
        } catch (Exception e) {
            throw new InvalidTransactionException("Impossible de vérifier l'accès au compte: " + e.getMessage());
        }
    }
}
