package com.banking.customer_service.controller;

import com.banking.customer_service.dto.ClientRequestDTO;
import com.banking.customer_service.dto.ClientResponseDTO;
import com.banking.customer_service.dto.KycRequestDTO;
import com.banking.customer_service.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController                          // expose des endpoints REST, reponses en JSON
@RequestMapping("/api/customers")       // prefixe commun a tous les endpoints (cf. contrat REST)
@RequiredArgsConstructor                // injection du service par constructeur (champ final)
public class ClientController {

    private final ClientService clientService;

    // POST /api/customers  -> cree un client, renvoie 201 Created
    @PostMapping
    public ResponseEntity<ClientResponseDTO> creerClient(@RequestBody ClientRequestDTO dto) {
        ClientResponseDTO cree = clientService.creerClient(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(cree);
    }

    // GET /api/customers/{id}  -> renvoie un client, 200 OK (ou 404 si introuvable)
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> getClient(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClient(id));
    }

    // GET /api/customers  -> liste tous les clients
    @GetMapping
    public ResponseEntity<List<ClientResponseDTO>> lister() {
        return ResponseEntity.ok(clientService.lister());
    }

    // PUT /api/customers/{id}  -> modifie un client
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> modifier(@PathVariable Long id, @RequestBody ClientRequestDTO dto) {
        return ResponseEntity.ok(clientService.modifier(id, dto));
    }

    // PATCH /api/customers/{id}/kyc  -> met a jour le statut KYC
    @PatchMapping("/{id}/kyc")
    public ResponseEntity<ClientResponseDTO> majKyc(@PathVariable Long id, @RequestBody KycRequestDTO dto) {
        return ResponseEntity.ok(clientService.majKyc(id, dto.statutKyc()));
    }
}
