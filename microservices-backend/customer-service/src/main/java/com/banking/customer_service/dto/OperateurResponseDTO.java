package com.banking.customer_service.dto;

import com.banking.customer_service.entity.TypeOperateur;

public record OperateurResponseDTO(
        Long id,
        String nom,
        TypeOperateur type,
        String code
) {
}
