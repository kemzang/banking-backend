package com.banking.transaction_service.controller;

import com.banking.transaction_service.dto.DepotRequestDTO;
import com.banking.transaction_service.dto.RetraitRequestDTO;
import com.banking.transaction_service.dto.TransactionResponseDTO;
import com.banking.transaction_service.dto.TransfertRequestDTO;
import com.banking.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponseDTO> deposit(
            @Valid @RequestBody DepotRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.depot(request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponseDTO> withdraw(
            @Valid @RequestBody RetraitRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.retrait(request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponseDTO> transfer(
            @Valid @RequestBody TransfertRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfert(request));
    }

    @GetMapping("/{id}")
    public TransactionResponseDTO getById(@PathVariable @Positive Long id) {
        return transactionService.getById(id);
    }

    @GetMapping
    public List<TransactionResponseDTO> getByAccount(
            @RequestParam @Positive Long accountId
    ) {
        return transactionService.getByCompte(accountId);
    }
}
