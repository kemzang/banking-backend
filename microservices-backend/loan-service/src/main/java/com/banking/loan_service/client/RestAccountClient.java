package com.banking.loan_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
public class RestAccountClient implements AccountClient {

    private final RestClient accountRestClient;

    public RestAccountClient(RestClient accountRestClient) {
        this.accountRestClient = accountRestClient;
    }

    @Override
    public void credit(Long accountId, BigDecimal amount, String motif) {
        try {
            Map<String, Object> body = Map.of(
                    "montant", amount,
                    "devise", "XAF",
                    "motif", motif
            );
            accountRestClient.post()
                    .uri("/{id}/credit", accountId)
                    .body(body)
                    .retrieve();
            log.info("Compte {} crédité de {} (prêt)", accountId, amount);
        } catch (RestClientResponseException e) {
            log.error("Erreur lors du crédit du compte {}: {}", accountId, e.getMessage());
            throw new RuntimeException("Impossible de créditer le compte: " + accountId, e);
        }
    }
}
