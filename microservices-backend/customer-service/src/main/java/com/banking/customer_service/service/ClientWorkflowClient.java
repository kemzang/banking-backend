package com.banking.customer_service.service;

import com.banking.customer_service.dto.ClientResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class ClientWorkflowClient {

    private final RestClient authClient;
    private final RestClient notificationClient;

    public ClientWorkflowClient(
            @Value("${services.auth.url:http://auth-service:8085}") String authUrl,
            @Value("${services.notification.url:http://notification-service:3000}") String notificationUrl) {
        this.authClient = RestClient.builder().baseUrl(authUrl).build();
        this.notificationClient = RestClient.builder().baseUrl(notificationUrl).build();
    }

    public void updateUserStatus(UUID userId, String status) {
        authClient.patch()
                .uri("/api/auth/internal/users/{id}/status", userId)
                .header("X-Internal-Service", "customer-service")
                .body(Map.of("status", status))
                .retrieve()
                .toBodilessEntity();
    }

    public void notifyOperator(ClientResponseDTO client) {
        sendNotification(Map.of(
                "type", "CLIENT_VALIDATION_REQUEST",
                "status", "UNREAD",
                "clientId", client.id(),
                "operatorId", client.operateurId(),
                "clientEmail", client.email(),
                "message", "Nouveau client en attente de validation : " + client.prenom() + " " + client.nom(),
                "route", "/operator/validations"
        ));
    }

    public void notifyClient(ClientResponseDTO client, String type, String message) {
        sendNotification(Map.of(
                "type", type,
                "status", "UNREAD",
                "clientId", client.id(),
                "operatorId", client.operateurId(),
                "clientEmail", client.email(),
                "message", message,
                "route", "/client/profile"
        ));
    }

    private void sendNotification(Map<String, Object> notification) {
        try {
            notificationClient.post()
                    .uri("/api/notifications")
                    .header("X-Internal-Service", "customer-service")
                    .body(notification)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            log.warn("Notification differee: {}", exception.getMessage());
        }
    }
}
