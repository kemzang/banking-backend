package com.banking.transaction_service.controller;

import com.banking.transaction_service.client.AccountClient;
import com.banking.transaction_service.client.CustomerClient;
import com.banking.transaction_service.dto.AccountResponseDTO;
import com.banking.transaction_service.dto.DepotRequestDTO;
import com.banking.transaction_service.dto.RetraitRequestDTO;
import com.banking.transaction_service.dto.TransactionResponseDTO;
import com.banking.transaction_service.dto.TransfertRequestDTO;
import com.banking.transaction_service.exception.ForbiddenException;
import com.banking.transaction_service.exception.UnauthorizedException;
import com.banking.transaction_service.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Depots, retraits, transferts et historique")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountClient accountClient;
    private final CustomerClient customerClient;

    public TransactionController(TransactionService transactionService,
                                  AccountClient accountClient,
                                  CustomerClient customerClient) {
        this.transactionService = transactionService;
        this.accountClient = accountClient;
        this.customerClient = customerClient;
    }

    @PostMapping("/deposit")
    @Operation(summary = "Effectuer un depot")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Depot valide"),
            @ApiResponse(responseCode = "400", description = "Requete invalide"),
            @ApiResponse(responseCode = "403", description = "Acces refuse"),
            @ApiResponse(responseCode = "502", description = "Erreur account-service"),
            @ApiResponse(responseCode = "503", description = "account-service indisponible")
    })
    public ResponseEntity<TransactionResponseDTO> deposit(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = DepotRequestDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {"compteId":1,"montant":50000,"devise":"XAF"}
                                            """
                            )
                    )
            )
            @Valid @RequestBody DepotRequestDTO request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles
    ) {
        verifierAccesCompte(request.compteId(), userEmail, userRoles);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.depot(request));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Effectuer un retrait")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Retrait valide"),
            @ApiResponse(responseCode = "400", description = "Requete invalide"),
            @ApiResponse(responseCode = "403", description = "Acces refuse"),
            @ApiResponse(responseCode = "409", description = "Solde ou devise incompatible")
    })
    public ResponseEntity<TransactionResponseDTO> withdraw(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RetraitRequestDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {"compteId":1,"montant":10000,"devise":"XAF"}
                                            """
                            )
                    )
            )
            @Valid @RequestBody RetraitRequestDTO request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles
    ) {
        verifierAccesCompte(request.compteId(), userEmail, userRoles);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.retrait(request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Effectuer un transfert")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfert valide"),
            @ApiResponse(responseCode = "400", description = "Requete invalide"),
            @ApiResponse(responseCode = "403", description = "Acces refuse"),
            @ApiResponse(responseCode = "409", description = "Solde ou devise incompatible"),
            @ApiResponse(responseCode = "502", description = "Erreur account-service")
    })
    public ResponseEntity<TransactionResponseDTO> transfer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TransfertRequestDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {"compteSourceId":1,"compteDestId":2,
                                            "montant":25000,"devise":"XAF",
                                            "motif":"Paiement fournisseur"}
                                            """
                            )
                    )
            )
            @Valid @RequestBody TransfertRequestDTO request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles
    ) {
        verifierAccesCompte(request.compteSourceId(), userEmail, userRoles);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfert(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction trouvee"),
            @ApiResponse(responseCode = "404", description = "Transaction introuvable")
    })
    public TransactionResponseDTO getById(
            @Parameter(example = "1") @PathVariable @Positive Long id
    ) {
        return transactionService.getById(id);
    }

    @GetMapping
    @Operation(summary = "Consulter l'historique d'un compte")
    @ApiResponse(responseCode = "200", description = "Historique retourne")
    public List<TransactionResponseDTO> getByAccount(
            @Parameter(example = "1") @RequestParam @Positive Long accountId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles
    ) {
        verifierAccesCompte(accountId, userEmail, userRoles);
        return transactionService.getByCompte(accountId);
    }

    private boolean estAdminOuOperateur(String roles) {
        if (roles == null || roles.isBlank()) return false;
        return roles.contains("ADMIN") || roles.contains("OPERATEUR");
    }

    private Long getClientIdFromEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Email utilisateur manquant");
        }
        return customerClient.getClientByEmail(email).id();
    }

    private void verifierAccesCompte(Long compteId, String userEmail, String userRoles) {
        if (estAdminOuOperateur(userRoles)) {
            return;
        }
        Long monClientId = getClientIdFromEmail(userEmail);
        AccountResponseDTO compte = accountClient.getById(compteId);
        if (!monClientId.equals(compte.clientId())) {
            throw new ForbiddenException("Accès refusé à ce compte");
        }
    }
}
