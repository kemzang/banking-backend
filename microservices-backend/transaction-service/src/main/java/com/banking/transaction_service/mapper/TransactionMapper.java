package com.banking.transaction_service.mapper;

import com.banking.transaction_service.dto.TransactionResponseDTO;
import com.banking.transaction_service.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponseDTO toResponse(Transaction transaction) {
        return new TransactionResponseDTO(
                transaction.getId(),
                transaction.getReference(),
                transaction.getType(),
                transaction.getMontant(),
                transaction.getDevise(),
                transaction.getCommission(),
                transaction.getStatut(),
                transaction.getMotif(),
                transaction.getDateOperation()
        );
    }
}
