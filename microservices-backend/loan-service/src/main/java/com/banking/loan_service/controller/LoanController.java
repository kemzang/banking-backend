package com.banking.loan_service.controller;

import com.banking.loan_service.dto.*;
import com.banking.loan_service.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/applications")
    public ResponseEntity<DemandePretResponseDTO> soumettreDemande(@RequestBody DemandePretRequestDTO request) {
        DemandePretResponseDTO response = loanService.soumettre(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/applications")
    public ResponseEntity<List<DemandePretResponseDTO>> getAllDemandes(
            @RequestParam(required = false) Long clientId) {
        if (clientId != null) {
            return ResponseEntity.ok(loanService.getDemandesByClientId(clientId));
        }
        return ResponseEntity.ok(loanService.getAllDemandes());
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<DemandePretResponseDTO> getDemandePret(@PathVariable Long id) {
        try {
            DemandePretResponseDTO response = loanService.getDemandePret(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/applications/{id}/decision")
    public ResponseEntity<Object> prendreDecision(@PathVariable Long id, @RequestBody DecisionRequestDTO decision) {
        Object response = loanService.decider(id, decision);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PretResponseDTO> getPret(@PathVariable Long id) {
        try {
            PretResponseDTO response = loanService.getPret(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<PretResponseDTO>> getAllPrets(
            @RequestParam(required = false) Long clientId) {
        if (clientId != null) {
            return ResponseEntity.ok(loanService.getPretsByClientId(clientId));
        }
        return ResponseEntity.ok(loanService.getAllPrets());
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<EcheancierDTO> getEcheancier(@PathVariable Long id) {
        EcheancierDTO response = loanService.getEcheancier(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/repay")
    public ResponseEntity<PretResponseDTO> rembourser(@PathVariable Long id, @RequestBody RemboursementRequestDTO request) {
        PretResponseDTO response = loanService.rembourser(id, request);
        return ResponseEntity.ok(response);
    }
}