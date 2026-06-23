package com.banking.loan_service.dto;

public record CustomerResponseDTO(
        Long id,
        String nom,
        String prenom,
        String email,
        String statutKyc,
        Long operateurId
) {
}
