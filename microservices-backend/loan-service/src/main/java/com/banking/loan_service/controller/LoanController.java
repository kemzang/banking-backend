package com.banking.loan_service.controller;

import com.banking.loan_service.dto.*;
import com.banking.loan_service.service.LoanService;
import com.banking.loan_service.service.LoanNotificationClient;
import com.banking.loan_service.client.CustomerClient;
import com.banking.loan_service.client.AccountClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final CustomerClient customerClient;
    private final AccountClient accountClient;
    private final LoanNotificationClient notificationClient;

    @PostMapping("/applications/mine")
    public ResponseEntity<DemandePretResponseDTO> soumettreMaDemande(
            @RequestBody ClientLoanRequestDTO request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Roles") String roles) {
        requireRole(roles, "CLIENT");
        CustomerResponseDTO customer = customerClient.getByEmail(email);
        verifierCompte(request.accountId(), customer, email, roles, null);
        DemandePretResponseDTO response = loanService.soumettre(new DemandePretRequestDTO(
                customer.id(), request.accountId(), customer.operateurId(),
                request.montantDemande(), request.dureeMois(), request.motif()));
        notificationClient.requested(response, customer.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/applications/mine")
    public ResponseEntity<java.util.List<DemandePretResponseDTO>> mesDemandes(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Roles") String roles) {
        requireRole(roles, "CLIENT");
        CustomerResponseDTO customer = customerClient.getByEmail(email);
        return ResponseEntity.ok(loanService.listerDemandesClient(customer.id()));
    }

    @GetMapping("/mine")
    public ResponseEntity<java.util.List<PretResponseDTO>> mesPrets(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Roles") String roles) {
        requireRole(roles, "CLIENT");
        CustomerResponseDTO customer = customerClient.getByEmail(email);
        return ResponseEntity.ok(loanService.listerPretsClient(customer.id()));
    }

    @PostMapping("/applications")
    public ResponseEntity<DemandePretResponseDTO> soumettreDemande(
            @RequestBody DemandePretRequestDTO request,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        verifierAccesClient(request.clientId(), email, roles, operatorId);
        CustomerResponseDTO customer = customerClient.getById(request.clientId());
        verifierCompte(request.accountId(), customer, email, roles, operatorId);
        DemandePretRequestDTO safeRequest = new DemandePretRequestDTO(customer.id(), request.accountId(),
                customer.operateurId(), request.montantDemande(), request.dureeMois(), request.motif());
        DemandePretResponseDTO response = loanService.soumettre(safeRequest);
        notificationClient.requested(response, customer.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping
    public ResponseEntity<DemandePretResponseDTO> soumettre(
            @RequestBody ClientLoanRequestDTO request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Roles") String roles) {
        return soumettreMaDemande(request, email, roles);
    }

    @GetMapping("/my")
    public ResponseEntity<java.util.List<DemandePretResponseDTO>> my(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Roles") String roles) {
        return mesDemandes(email, roles);
    }

    @GetMapping("/pending")
    public ResponseEntity<java.util.List<DemandePretResponseDTO>> pending(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        boolean admin = hasRole(roles, "ADMIN_PLATFORM");
        if (!admin && !hasRole(roles, "OPERATOR_ADMIN") && !hasRole(roles, "OPERATOR_AGENT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
        }
        if (!admin && operatorId == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operateur manquant");
        return ResponseEntity.ok(loanService.listerEnAttente(operatorId, admin));
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<DemandePretResponseDTO> getDemandePret(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        try {
            DemandePretResponseDTO response = loanService.getDemandePret(id);
            verifierAccesClient(response.clientId(), email, roles, operatorId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/applications/{id}/decision")
    public ResponseEntity<Object> prendreDecision(
            @PathVariable Long id,
            @RequestBody DecisionRequestDTO decision,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        requireAnyRole(roles, "ADMIN_PLATFORM", "OPERATOR_ADMIN");
        verifierAccesClient(loanService.getDemandePret(id).clientId(), email, roles, operatorId);
        Object response = loanService.decider(id, decision);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PretResponseDTO> getPret(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        try {
            PretResponseDTO response = loanService.getPret(id);
            verifierAccesClient(response.clientId(), email, roles, operatorId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<EcheancierDTO> getEcheancier(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        verifierAccesClient(loanService.getPret(id).clientId(), email, roles, operatorId);
        EcheancierDTO response = loanService.getEcheancier(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/repay")
    public ResponseEntity<PretResponseDTO> rembourser(
            @PathVariable Long id,
            @RequestBody RemboursementRequestDTO request,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        verifierAccesClient(loanService.getPret(id).clientId(), email, roles, operatorId);
        PretResponseDTO response = loanService.rembourser(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<Object> approve(@PathVariable Long id, @RequestBody DecisionRequestDTO decision,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        requireAnyRole(roles, "ADMIN_PLATFORM", "OPERATOR_ADMIN");
        DemandePretResponseDTO demande = loanService.getDemandePret(id);
        verifierAccesClient(demande.clientId(), email, roles, operatorId);
        return ResponseEntity.ok(loanService.decider(id,
                new DecisionRequestDTO(true, decision.tauxInteret(), demande.accountId(), null)));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<Object> reject(@PathVariable Long id, @RequestBody LoanRejectRequestDTO request,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        requireAnyRole(roles, "ADMIN_PLATFORM", "OPERATOR_ADMIN");
        DemandePretResponseDTO demande = loanService.getDemandePret(id);
        verifierAccesClient(demande.clientId(), email, roles, operatorId);
        return ResponseEntity.ok(loanService.decider(id,
                new DecisionRequestDTO(false, null, demande.accountId(), request.reason())));
    }

    private void verifierAccesClient(Long clientId, String email, String roles, Long operatorId) {
        if (hasRole(roles, "ADMIN_PLATFORM")) {
            return;
        }
        CustomerResponseDTO customer = customerClient.getById(clientId);
        if (hasRole(roles, "OPERATOR_ADMIN") || hasRole(roles, "OPERATOR_AGENT")) {
            if (operatorId == null || !operatorId.equals(customer.operateurId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Client rattache a un autre operateur");
            }
            return;
        }
        if (hasRole(roles, "CLIENT") && email != null && email.equalsIgnoreCase(customer.email())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse a ce pret");
    }

    private void verifierCompte(Long accountId, CustomerResponseDTO customer, String email, String roles, Long operatorId) {
        if (accountId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountId est obligatoire");
        AccountResponseDTO account = accountClient.getById(accountId, email == null ? "internal@bank" : email,
                roles == null ? "ADMIN_PLATFORM" : roles, operatorId);
        if (!customer.id().equals(account.clientId()) || !customer.operateurId().equals(account.operateurId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte non rattache a ce client/operateur");
        }
        if (!"ACTIF".equals(account.statut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le compte bancaire doit etre actif");
        }
    }

    private boolean hasRole(String roles, String expected) {
        return roles != null && Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(expected::equals);
    }

    private void requireRole(String roles, String expected) {
        if (!hasRole(roles, expected)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role " + expected + " requis");
        }
    }

    private void requireAnyRole(String roles, String... expectedRoles) {
        if (Arrays.stream(expectedRoles).noneMatch(role -> hasRole(roles, role))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action reservee aux operateurs");
        }
    }
}
