package com.banking.loan_service.controller;

import com.banking.loan_service.client.CustomerClient;
import com.banking.loan_service.dto.ClientLoanRequestDTO;
import com.banking.loan_service.dto.CustomerResponseDTO;
import com.banking.loan_service.dto.DemandePretRequestDTO;
import com.banking.loan_service.dto.DemandePretResponseDTO;
import com.banking.loan_service.dto.DecisionRequestDTO;
import com.banking.loan_service.entity.StatutDemande;
import com.banking.loan_service.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoanControllerSecurityTest {

    @Test
    void clientCannotDecideLoanEvenWhenItBelongsToThem() {
        LoanService service = mock(LoanService.class);
        CustomerClient customers = mock(CustomerClient.class);
        LoanController controller = new LoanController(service, customers);

        assertThatThrownBy(() -> controller.prendreDecision(
                10L,
                new DecisionRequestDTO(true, BigDecimal.TEN, 5L, null),
                "client@example.cm",
                "CLIENT",
                null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(403));

        verify(service, never()).decider(any(), any());
    }

    @Test
    void clientLoanRequestUsesCustomerResolvedFromJwtEmail() {
        LoanService service = mock(LoanService.class);
        CustomerClient customers = mock(CustomerClient.class);
        LoanController controller = new LoanController(service, customers);
        CustomerResponseDTO customer = new CustomerResponseDTO(
                42L, "Client", "Marie", "marie@example.cm", "EN_ATTENTE", 1L);
        DemandePretResponseDTO saved = new DemandePretResponseDTO(
                7L, 42L, BigDecimal.valueOf(500_000), 12,
                BigDecimal.valueOf(.2), StatutDemande.SOUMISE);
        when(customers.getByEmail("marie@example.cm")).thenReturn(customer);
        when(service.soumettre(any(DemandePretRequestDTO.class))).thenReturn(saved);

        var response = controller.soumettreMaDemande(
                new ClientLoanRequestDTO(BigDecimal.valueOf(500_000), 12, "Equipement"),
                "marie@example.cm",
                "CLIENT"
        );

        assertThat(response.getBody().clientId()).isEqualTo(42L);
        verify(service).soumettre(new DemandePretRequestDTO(
                42L, BigDecimal.valueOf(500_000), 12, "Equipement"));
    }
}
