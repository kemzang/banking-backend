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

    // GET /api/customers/by-email/{email}  -> renvoie un client par email
    // DOIT etre avant /{id} sinon "by-email" est interprete comme un id
    @GetMapping("/by-email/{email}")
    public ResponseEntity<ClientResponseDTO> getClientParEmail(
            @PathVariable String email,
            @RequestHeader(value = "X-User-Email", required = false) String currentEmail,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (hasRole(roles, "CLIENT") && (currentEmail == null || !currentEmail.equalsIgnoreCase(email))) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Acces a un autre profil refuse");
        }
        return ResponseEntity.ok(clientService.getClientParEmail(email));
    }

    // GET /api/customers/{id}  -> renvoie un client, 200 OK (ou 404 si introuvable)
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> getClient(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        ClientResponseDTO client = clientService.getClient(id);
        verifierAcces(client, email, roles, operatorId);
        return ResponseEntity.ok(client);
    }

    // GET /api/customers  -> liste tous les clients
    @GetMapping
    public ResponseEntity<List<ClientResponseDTO>> lister(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId,
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        if (hasRole(roles, "ADMIN_PLATFORM")) {
            return ResponseEntity.ok(clientService.lister());
        }
        if (hasRole(roles, "OPERATOR_ADMIN") || hasRole(roles, "OPERATOR_AGENT")) {
            if (operatorId == null) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Identite operateur manquante");
            }
            return ResponseEntity.ok(clientService.listerParOperateur(operatorId));
        }
        if (hasRole(roles, "CLIENT") && email != null) {
            return ResponseEntity.ok(List.of(clientService.getClientParEmail(email)));
        }
        throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
    }

    private boolean hasRole(String roles, String role) {
        return roles != null && java.util.Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(role::equals);
    }

    // PUT /api/customers/{id}  -> modifie un client
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> modifier(
            @PathVariable Long id,
            @RequestBody ClientRequestDTO dto,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        ClientResponseDTO client = clientService.getClient(id);
        verifierAcces(client, email, roles, operatorId);
        if (hasRole(roles, "CLIENT")) {
            return ResponseEntity.ok(clientService.modifierInformationsPersonnelles(id, dto));
        }
        return ResponseEntity.ok(clientService.modifier(id, dto));
    }

    // PATCH /api/customers/{id}/kyc  -> met a jour le statut KYC
    @PatchMapping("/{id}/kyc")
    public ResponseEntity<ClientResponseDTO> majKyc(
            @PathVariable Long id,
            @RequestBody KycRequestDTO dto,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        if (hasRole(roles, "CLIENT")) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Un client ne peut pas valider son KYC");
        }
        ClientResponseDTO client = clientService.getClient(id);
        if (hasRole(roles, "OPERATOR_ADMIN") || hasRole(roles, "OPERATOR_AGENT")) {
            if (operatorId == null || !operatorId.equals(client.operateurId())) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Client rattache a un autre operateur");
            }
        } else if (!hasRole(roles, "ADMIN_PLATFORM")) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Validation KYC reservee aux operateurs");
        }
        return ResponseEntity.ok(clientService.majKyc(id, dto.statutKyc()));
    }

    private void verifierAcces(ClientResponseDTO client, String email, String roles, Long operatorId) {
        if (roles == null) {
            return; // Appel interservice direct, non expose par la gateway sans JWT.
        }
        if (hasRole(roles, "ADMIN_PLATFORM")) return;
        if (hasRole(roles, "CLIENT") && email != null && email.equalsIgnoreCase(client.email())) return;
        if ((hasRole(roles, "OPERATOR_ADMIN") || hasRole(roles, "OPERATOR_AGENT"))
                && operatorId != null && operatorId.equals(client.operateurId())) return;
        throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse a ce client");
    }
}
