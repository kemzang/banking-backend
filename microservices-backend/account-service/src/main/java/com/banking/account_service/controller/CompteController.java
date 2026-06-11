package com.banking.account_service.controller;

import com.banking.account_service.dto.*;
import com.banking.account_service.service.CompteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // POST /api/accounts  → ouvrir un compte (201)
    @PostMapping
    public ResponseEntity<CompteResponseDTO> ouvrir(@RequestBody CompteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compteService.ouvrirCompte(dto));
    }

    // GET /api/accounts/{id}  → détails d'un compte (200 / 404)
    @GetMapping("/{id}")
    public ResponseEntity<CompteResponseDTO> getCompte(@PathVariable Long id) {
        return ResponseEntity.ok(compteService.getCompte(id));
    }

    // GET /api/accounts  → liste (tous ou filtrés par clientId)
    // Exemples : /api/accounts  ou  /api/accounts?clientId=5
    @GetMapping
    public ResponseEntity<List<CompteResponseDTO>> lister(
            @RequestParam(required = false) Long clientId) {
        List<CompteResponseDTO> result = (clientId != null)
                ? compteService.listerParClient(clientId)
                : compteService.listerTous();
        return ResponseEntity.ok(result);
    }

    // GET /api/accounts/{id}/balance  → solde courant
    @GetMapping("/{id}/balance")
    public ResponseEntity<SoldeResponseDTO> getSolde(@PathVariable Long id) {
        return ResponseEntity.ok(compteService.getSolde(id));
    }

    // PATCH /api/accounts/{id}/suspend  → suspendre
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<CompteResponseDTO> suspendre(@PathVariable Long id) {
        return ResponseEntity.ok(compteService.suspendre(id));
    }

    // PATCH /api/accounts/{id}/close  → clôturer (solde doit être 0)
    @PatchMapping("/{id}/close")
    public ResponseEntity<CompteResponseDTO> cloturer(@PathVariable Long id) {
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
}
