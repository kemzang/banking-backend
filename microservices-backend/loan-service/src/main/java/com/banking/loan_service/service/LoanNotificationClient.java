package com.banking.loan_service.service;

import com.banking.loan_service.dto.DemandePretResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Component
public class LoanNotificationClient {
    private final RestClient client;
    public LoanNotificationClient(@Value("${services.notification.url:http://notification-service:3000}") String url) {
        this.client = RestClient.builder().baseUrl(url).build();
    }
    public void requested(DemandePretResponseDTO loan, String email) {
        try {
            client.post().uri("/api/notifications").header("X-Internal-Service", "loan-service")
                    .body(Map.of("type", "LOAN_REQUESTED", "status", "UNREAD", "loanRequestId", loan.id(),
                            "clientId", loan.clientId(), "accountId", loan.accountId(), "operatorId", loan.operatorId(),
                            "clientEmail", email, "message", "Nouvelle demande de pret en attente.",
                            "route", "/operator/loans"))
                    .retrieve().toBodilessEntity();
        } catch (Exception ignored) { }
    }
}
