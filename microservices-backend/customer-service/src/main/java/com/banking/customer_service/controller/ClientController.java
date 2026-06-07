package com.banking.customer_service.controller;

import com.banking.customer_service.dto.ClientRequestDTO;
import com.banking.customer_service.dto.ClientResponseDTO;
import com.banking.customer_service.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
