package com.banking.transaction_service.service;

import com.banking.transaction_service.dto.DepotRequestDTO;
import com.banking.transaction_service.dto.RetraitRequestDTO;
import com.banking.transaction_service.dto.TransactionResponseDTO;
import com.banking.transaction_service.dto.TransfertRequestDTO;

import java.util.List;

public interface TransactionService {

    TransactionResponseDTO depot(DepotRequestDTO request);

    TransactionResponseDTO retrait(RetraitRequestDTO request);

    TransactionResponseDTO transfert(TransfertRequestDTO request);

    TransactionResponseDTO getById(Long id);

    List<TransactionResponseDTO> getByCompte(Long accountId);
}
