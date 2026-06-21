package com.banking.account_service.controller;

import com.banking.account_service.client.CustomerClient;
import com.banking.account_service.dto.*;
import com.banking.account_service.service.CompteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    // POST /api/accounts  → ouvrir un compte (201)
    @PostMapping
    public ResponseEntity<CompteResponseDTO> ouvrir(@RequestBody CompteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compteService.ouvrirCompte(dto));
    }

    // GET /api/accounts/{id}  → détails d'un compte (200 / 404)
    // NOTE: la vérification d'accès est faite par transaction-service avant l'appel.
    // Ce endpoint est appelé par transaction-service avec le header X-User-Email
    // pour logs/audit, mais la vérification principale se fait côté transaction-service.
    @GetMapping("/{id}")
    public ResponseEntity<CompteResponseDTO> getCompte(
            @PathVariable Long id) {
        return ResponseEntity.ok(compteService.getCompte(id));
    }

    // GET /api/accounts  → liste (filtrée par sécurité selon le rôle)
    @GetMapping
    public ResponseEntity<List<CompteResponseDTO>> lister(
            @RequestParam(required = false) Long clientId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        
        List<CompteResponseDTO> result;
        
        if (estAdminOuOperateur(userRoles)) {
            // Admin/Operateur : peut tout voir ou filtrer par clientId
            result = (clientId != null)
                    ? compteService.listerParClient(clientId)
                    : compteService.listerTous();
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
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        CompteResponseDTO compte = compteService.getCompte(id);
        verifierAccesCompte(compte.clientId(), userEmail, userRoles);
        return ResponseEntity.ok(compteService.getSolde(id));
    }

    // PATCH /api/accounts/{id}/suspend  → suspendre (admin/operateur uniquement)
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<CompteResponseDTO> suspendre(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        verifierRoleAdminOuOperateur(userRoles);
        return ResponseEntity.ok(compteService.suspendre(id));
    }

    // PATCH /api/accounts/{id}/close  → clôturer (admin/operateur uniquement)
    @PatchMapping("/{id}/close")
    public ResponseEntity<CompteResponseDTO> cloturer(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        verifierRoleAdminOuOperateur(userRoles);
        return ResponseEntity.ok(compteService.cloturer(id));
    }

    // POST /api/accounts/{id}/credit  → créditer (usage interne)
    @PostMapping("/{id}/credit")
    public ResponseEntity<SoldeResponseDTO> crediter(
            @PathVariable Long id, @RequestBody MouvementDTO dto) {
        return ResponseEntity.ok(compteService.crediter(id, dto));
    }

    // POST /api/accounts/{id}/debit  → débiter (usage interne)
    @PostMapping("/{id}/debit")
    public ResponseEntity<SoldeResponseDTO> debiter(
            @PathVariable Long id, @RequestBody MouvementDTO dto) {
        return ResponseEntity.ok(compteService.debiter(id, dto));
    }

    // --- Méthodes de vérification d'accès ---

    private boolean estAdminOuOperateur(String roles) {
        if (roles == null || roles.isBlank()) return false;
        return roles.contains("ADMIN") || roles.contains("OPERATEUR");
    }

    private Long getClientIdFromEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email utilisateur manquant");
        }
        try {
            return customerClient.getClientByEmail(email).id();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Client non trouvé pour cet email");
        }
    }

    private void verifierAccesCompte(Long compteClientId, String userEmail, String userRoles) {
        if (estAdminOuOperateur(userRoles)) {
            return; // Admin/Operateur a accès à tout
        }
        Long monClientId = getClientIdFromEmail(userEmail);
        if (!compteClientId.equals(monClientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé à ce compte");
        }
    }

    private void verifierRoleAdminOuOperateur(String roles) {
        if (!estAdminOuOperateur(roles)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Opération réservée aux administrateurs");
        }
    }
}
