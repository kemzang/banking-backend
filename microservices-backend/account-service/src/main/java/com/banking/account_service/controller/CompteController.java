package com.banking.account_service.controller;

import com.banking.account_service.client.CustomerClient;
import com.banking.account_service.dto.*;
import com.banking.account_service.service.CompteService;
import com.banking.account_service.service.AccountNotificationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

/**
 * Endpoints REST de l'account-service.
 * Tout passe par la Gateway sur :8080 → /api/accounts/**
 *
 * Endpoints internes (crédit/débit) : appelés uniquement par transaction-service,
 * jamais directement par le frontend.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class CompteController {

    private final CompteService compteService;
    private final CustomerClient customerClient;
    private final AccountNotificationClient notificationClient;

    // POST /api/accounts  → ouvrir un compte (201)
    @PostMapping
    public ResponseEntity<CompteResponseDTO> ouvrir(
            @RequestBody CompteRequestDTO dto,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        CompteRequestDTO effective = dto;
        ClientResponseDTO owner;
        if (hasRole(roles, "CLIENT")) {
            owner = getClientFromEmail(userEmail);
            if (!"VALIDE".equals(owner.statutKyc())) {
                throw new ResponseStatusException(HttpStatus.LOCKED, "Profil client non valide");
            }
            effective = new CompteRequestDTO(owner.id(), owner.operateurId(), dto.type(), dto.devise(),
                    dto.plafondJournalier(), dto.decouvertAutorise());
        } else {
            verifierRoleAdminOuOperateur(roles);
            owner = customerClient.getClientById(dto.clientId());
            effective = new CompteRequestDTO(owner.id(), owner.operateurId(), dto.type(), dto.devise(),
                    dto.plafondJournalier(), dto.decouvertAutorise());
        }
        if (estOperateur(roles)) {
            requireOperatorId(operatorId);
            if (!operatorId.equals(effective.operateurId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Impossible d'ouvrir un compte pour un autre operateur");
            }
        }
        CompteResponseDTO created = compteService.ouvrirCompte(effective);
        notificationClient.openingRequested(created, owner.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<CompteResponseDTO>> pending(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        if (!estAdmin(roles) && !estOperateur(roles)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
        if (!estAdmin(roles)) requireOperatorId(operatorId);
        return ResponseEntity.ok(compteService.listerEnAttente(operatorId, estAdmin(roles)));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<CompteResponseDTO> activer(@PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        verifierRoleDecision(roles);
        verifierOperatorScope(compteService.getCompte(id), roles, operatorId);
        return ResponseEntity.ok(compteService.activer(id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<CompteResponseDTO> rejeter(@PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        verifierRoleDecision(roles);
        verifierOperatorScope(compteService.getCompte(id), roles, operatorId);
        return ResponseEntity.ok(compteService.rejeter(id));
    }

    // GET /api/accounts/{id}  → détails d'un compte (200 / 404)
    @GetMapping("/{id}")
    public ResponseEntity<CompteResponseDTO> getCompte(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        CompteResponseDTO compte = compteService.getCompte(id);
        verifierAccesCompte(compte, userEmail, userRoles, operatorId);
        return ResponseEntity.ok(compte);
    }

    // GET /api/accounts  → liste (filtrée par sécurité selon le rôle)
    @GetMapping
    public ResponseEntity<List<CompteResponseDTO>> lister(
            @RequestParam(required = false) Long clientId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        
        List<CompteResponseDTO> result;
        
        if (estAdmin(userRoles)) {
            result = (clientId != null)
                    ? compteService.listerParClient(clientId)
                    : compteService.listerTous();
        } else if (estOperateur(userRoles)) {
            requireOperatorId(operatorId);
            List<CompteResponseDTO> comptes = (clientId != null)
                    ? compteService.listerParClient(clientId)
                    : compteService.listerTous();
            result = comptes.stream()
                    .filter(compte -> operatorId.equals(compte.operateurId()))
                    .toList();
        } else {
            // Client : ne voit que ses propres comptes
            Long monClientId = getClientIdFromEmail(userEmail);
            result = compteService.listerParClient(monClientId);
        }
        
        return ResponseEntity.ok(result);
    }

    // GET /api/accounts/{id}/balance  → solde courant
    @GetMapping("/{id}/balance")
    public ResponseEntity<SoldeResponseDTO> getSolde(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        CompteResponseDTO compte = compteService.getCompte(id);
        verifierAccesCompte(compte, userEmail, userRoles, operatorId);
        return ResponseEntity.ok(compteService.getSolde(id));
    }

    // PATCH /api/accounts/{id}/suspend  → suspendre (admin/operateur uniquement)
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<CompteResponseDTO> suspendre(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        verifierRoleAdminOuOperateur(userRoles);
        verifierOperatorScope(compteService.getCompte(id), userRoles, operatorId);
        return ResponseEntity.ok(compteService.suspendre(id));
    }

    // PATCH /api/accounts/{id}/close  → clôturer (admin/operateur uniquement)
    @PatchMapping("/{id}/close")
    public ResponseEntity<CompteResponseDTO> cloturer(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        verifierRoleAdminOuOperateur(userRoles);
        verifierOperatorScope(compteService.getCompte(id), userRoles, operatorId);
        return ResponseEntity.ok(compteService.cloturer(id));
    }

    // POST /api/accounts/{id}/credit  → créditer (usage interne)
    @PostMapping("/{id}/credit")
    public ResponseEntity<SoldeResponseDTO> crediter(
            @PathVariable Long id,
            @RequestBody MouvementDTO dto,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        refuserClientSurOperationInterne(roles);
        return ResponseEntity.ok(compteService.crediter(id, dto));
    }

    // POST /api/accounts/{id}/debit  → débiter (usage interne)
    @PostMapping("/{id}/debit")
    public ResponseEntity<SoldeResponseDTO> debiter(
            @PathVariable Long id,
            @RequestBody MouvementDTO dto,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        refuserClientSurOperationInterne(roles);
        return ResponseEntity.ok(compteService.debiter(id, dto));
    }

    // --- Méthodes de vérification d'accès ---

    private boolean estAdminOuOperateur(String roles) {
        return estAdmin(roles) || estOperateur(roles);
    }

    private boolean estAdmin(String roles) {
        return hasRole(roles, "ADMIN_PLATFORM") || hasRole(roles, "ADMIN");
    }

    private boolean estOperateur(String roles) {
        return hasRole(roles, "OPERATOR_ADMIN")
                || hasRole(roles, "OPERATOR_AGENT")
                || hasRole(roles, "OPERATEUR");
    }

    private boolean hasRole(String roles, String expectedRole) {
        return roles != null && Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(expectedRole::equals);
    }

    private Long getClientIdFromEmail(String email) {
        return getClientFromEmail(email).id();
    }

    private ClientResponseDTO getClientFromEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email utilisateur manquant");
        }
        try {
            return customerClient.getClientByEmail(email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Client non trouvé pour cet email");
        }
    }

    private void verifierAccesCompte(
            CompteResponseDTO compte,
            String userEmail,
            String userRoles,
            Long operatorId) {
        if (estAdmin(userRoles)) {
            return;
        }
        if (estOperateur(userRoles)) {
            verifierOperatorScope(compte, userRoles, operatorId);
            return;
        }
        if (!compte.clientId().equals(getClientIdFromEmail(userEmail))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse a ce compte");
        }
    }

    private void verifierRoleDecision(String roles) {
        if (!estAdmin(roles) && !hasRole(roles, "OPERATOR_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Decision reservee a OPERATOR_ADMIN");
        }
    }

    private void verifierOperatorScope(CompteResponseDTO compte, String roles, Long operatorId) {
        if (!estOperateur(roles)) {
            return;
        }
        requireOperatorId(operatorId);
        if (!operatorId.equals(compte.operateurId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte rattache a un autre operateur");
        }
    }

    private void requireOperatorId(Long operatorId) {
        if (operatorId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Identite operateur manquante");
        }
    }

    private void verifierRoleAdminOuOperateur(String roles) {
        if (!estAdminOuOperateur(roles)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Opération réservée aux administrateurs");
        }
    }

    private void refuserClientSurOperationInterne(String roles) {
        if (hasRole(roles, "CLIENT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operation de solde interne interdite au client");
        }
    }
}
