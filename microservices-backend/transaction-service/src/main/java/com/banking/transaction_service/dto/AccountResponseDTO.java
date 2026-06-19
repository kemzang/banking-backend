package com.banking.transaction_service.dto;

public record AccountResponseDTO(
        Long id,
        Long clientId,
        Long operateurId,
        String devise
) {
}
