package com.banking.transaction_service.client;

import com.banking.transaction_service.dto.AccountAmountRequestDTO;
import com.banking.transaction_service.dto.AccountResponseDTO;
import com.banking.transaction_service.exception.AccountServiceException;
import com.banking.transaction_service.exception.DeviseIncompatibleException;
import com.banking.transaction_service.exception.SoldeInsuffisantException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
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
    private final CircuitBreaker circuitBreaker;

    public RestAccountClient(RestClient accountRestClient, ObjectMapper objectMapper,
                             CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.accountRestClient = accountRestClient;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerFactory.create("account");
    }

    @Override
    public AccountResponseDTO getById(Long accountId) {
        // Protege par circuit breaker : si account-service est en panne, on echoue vite.
        return circuitBreaker.run(() -> doGetById(accountId), this::fallback);
    }

    @Override
    public void credit(Long accountId, BigDecimal amount) {
        circuitBreaker.run(() -> { updateBalance(accountId, amount, "credit"); return null; }, t -> fallback(t));
    }

    @Override
    public void debit(Long accountId, BigDecimal amount) {
        circuitBreaker.run(() -> { updateBalance(accountId, amount, "debit"); return null; }, t -> fallback(t));
    }

    // Fallback du circuit breaker. Les exceptions metier (solde, devise...) sont
    // declarees "ignore-exceptions" (application.yml) -> elles n'arrivent pas ici.
    // Ici on traite donc : panne reseau / 4xx-5xx account / circuit ouvert.
    private <T> T fallback(Throwable t) {
        if (t instanceof AccountServiceException ase) {
            throw ase;
        }
        throw new AccountServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                "account-service est indisponible (circuit ouvert)", t);
    }

    private AccountResponseDTO doGetById(Long accountId) {
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
