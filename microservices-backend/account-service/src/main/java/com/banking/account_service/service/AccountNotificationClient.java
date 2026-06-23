package com.banking.account_service.service;

import com.banking.account_service.dto.CompteResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Component
public class AccountNotificationClient {
    private final RestClient client;
    public AccountNotificationClient(@Value("${services.notification.url:http://notification-service:3000}") String url) {
        this.client = RestClient.builder().baseUrl(url).build();
    }
    public void openingRequested(CompteResponseDTO account, String email) {
        try {
            client.post().uri("/api/notifications").header("X-Internal-Service", "account-service")
                    .body(Map.of("type", "ACCOUNT_OPENING_REQUESTED", "status", "UNREAD",
                            "accountId", account.id(), "clientId", account.clientId(),
                            "operatorId", account.operateurId(), "clientEmail", email,
                            "message", "Nouveau compte en attente d'activation.", "route", "/operator/accounts"))
                    .retrieve().toBodilessEntity();
        } catch (Exception ignored) { }
    }
}
