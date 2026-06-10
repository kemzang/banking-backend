package com.banking.transaction_service.client;

import com.banking.transaction_service.dto.AccountAmountRequestDTO;
import com.banking.transaction_service.dto.AccountResponseDTO;
import com.banking.transaction_service.exception.AccountServiceException;
import com.banking.transaction_service.exception.DeviseIncompatibleException;
import com.banking.transaction_service.exception.SoldeInsuffisantException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

@Component
public class RestAccountClient implements AccountClient {

    private final RestClient accountRestClient;
    private final ObjectMapper objectMapper;

    public RestAccountClient(RestClient accountRestClient, ObjectMapper objectMapper) {
        this.accountRestClient = accountRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AccountResponseDTO getById(Long accountId) {
        try {
            return accountRestClient.get()
                    .uri("/api/accounts/{accountId}", accountId)
                    .retrieve()
                    .body(AccountResponseDTO.class);
        } catch (RestClientResponseException exception) {
            throw toAccountServiceException(exception);
        } catch (ResourceAccessException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public void credit(Long accountId, BigDecimal amount) {
        updateBalance(accountId, amount, "credit");
    }

    @Override
    public void debit(Long accountId, BigDecimal amount) {
        updateBalance(accountId, amount, "debit");
    }

    private void updateBalance(Long accountId, BigDecimal amount, String operation) {
        try {
            accountRestClient.post()
                    .uri("/api/accounts/{accountId}/{operation}", accountId, operation)
                    .body(new AccountAmountRequestDTO(amount))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            String message = extractMessage(exception);
            if (exception.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                String normalizedMessage = message.toLowerCase();
                if (normalizedMessage.contains("devise")) {
                    throw new DeviseIncompatibleException(message);
                }
                if ("debit".equals(operation) && normalizedMessage.contains("solde")) {
                    throw new SoldeInsuffisantException("Solde insuffisant");
                }
            }
            throw toAccountServiceException(exception);
        } catch (ResourceAccessException exception) {
            throw unavailable(exception);
        }
    }

    private AccountServiceException toAccountServiceException(
            RestClientResponseException exception
    ) {
        return new AccountServiceException(
                HttpStatus.BAD_GATEWAY,
                extractMessage(exception),
                exception
        );
    }

    private AccountServiceException unavailable(ResourceAccessException exception) {
        return new AccountServiceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "account-service est indisponible",
                exception
        );
    }

    private String extractMessage(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return "L'operation sur le compte a ete refusee";
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            String message = node.path("message").asText();
            return message.isBlank() ? body : message;
        } catch (Exception ignored) {
            return body;
        }
    }
}
