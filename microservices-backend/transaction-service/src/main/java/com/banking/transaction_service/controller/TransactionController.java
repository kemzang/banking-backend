package com.banking.transaction_service.controller;

import com.banking.transaction_service.client.AccountClient;
import com.banking.transaction_service.client.CustomerClient;
import com.banking.transaction_service.dto.AccountResponseDTO;
import com.banking.transaction_service.dto.DepotRequestDTO;
import com.banking.transaction_service.dto.RetraitRequestDTO;
import com.banking.transaction_service.dto.TransactionResponseDTO;
import com.banking.transaction_service.dto.TransfertRequestDTO;
import com.banking.transaction_service.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

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
            HttpServletRequest httpRequest
    ) {
        verifierAccesCompte(request.compteId(), httpRequest);
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
            HttpServletRequest httpRequest
    ) {
        verifierAccesCompte(request.compteId(), httpRequest);
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
            HttpServletRequest httpRequest
    ) {
        verifierAccesCompte(request.compteSourceId(), httpRequest);
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
            @Parameter(example = "1") @PathVariable @Positive Long id,
            HttpServletRequest httpRequest
    ) {
        TransactionResponseDTO transaction = transactionService.getById(id);
        Long accountId = transaction.compteSourceId() != null
                ? transaction.compteSourceId()
                : transaction.compteDestId();
        if (accountId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Transaction sans compte accessible");
        }
        verifierAccesCompte(accountId, httpRequest);
        return transaction;
    }

    @GetMapping
    @Operation(summary = "Consulter l'historique d'un compte")
    @ApiResponse(responseCode = "200", description = "Historique retourne")
    public List<TransactionResponseDTO> getByAccount(
            @Parameter(example = "1") @RequestParam @Positive Long accountId,
            HttpServletRequest httpRequest
    ) {
        verifierAccesCompte(accountId, httpRequest);
        return transactionService.getByCompte(accountId);
    }

    // ── Verification d'acces (lit le JWT via le SecurityContext) ──

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    private List<String> getCurrentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return List.of();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());
    }

    private Long getCurrentOperatorId(HttpServletRequest request) {
        Object op = request.getAttribute("operatorId");
        if (op instanceof Long l) return l;
        if (op instanceof Number n) return n.longValue();
        return null;
    }

    private boolean estAdmin(List<String> roles) {
        return roles.contains("ADMIN_PLATFORM") || roles.contains("ADMIN");
    }

    private boolean estOperateur(List<String> roles) {
        return roles.contains("OPERATOR_ADMIN")
                || roles.contains("OPERATOR_AGENT")
                || roles.contains("OPERATEUR");
    }

    private Long getClientIdFromEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email utilisateur manquant");
        }
        return customerClient.getClientByEmail(email).id();
    }

    private void verifierAccesCompte(Long compteId, HttpServletRequest httpRequest) {
        String userEmail = getCurrentUserEmail();
        List<String> userRoles = getCurrentRoles();
        Long operatorId = getCurrentOperatorId(httpRequest);

        // Pas d'auth du tout (route publique mal appellee)
        if (userEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }

        // ADMIN : acces total
        if (estAdmin(userRoles)) {
            return;
        }

        AccountResponseDTO compte = accountClient.getById(compteId);
        if (compte == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable");
        }

        // OPERATEUR : doit etre rattache au meme operateur que le compte
        if (estOperateur(userRoles)) {
            if (operatorId == null || !operatorId.equals(compte.operateurId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte rattache a un autre operateur");
            }
            return;
        }

        // CLIENT : doit etre le proprietaire du compte
        Long monClientId = getClientIdFromEmail(userEmail);
        if (compte.clientId() == null || !monClientId.equals(compte.clientId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé à ce compte");
        }
    }
}
