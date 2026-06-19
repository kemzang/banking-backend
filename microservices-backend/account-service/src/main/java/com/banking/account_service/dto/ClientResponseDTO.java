package com.banking.account_service.dto;

public record ClientResponseDTO(
        Long id,
        String nom,
        String prenom,
        String email,
        String statutKyc,
        Long operateurId
) {
}
