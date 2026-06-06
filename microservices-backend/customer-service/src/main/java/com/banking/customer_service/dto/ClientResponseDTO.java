package com.banking.customer_service.dto;

import com.banking.customer_service.entity.StatutKyc;

// Ce qu'on RENVOIE au client (on choisit d'exposer le minimum utile).
public record ClientResponseDTO(
        Long id,
        String nom,
        String prenom,
        String email,
        StatutKyc statutKyc,
        Long operateurId
) {
}
