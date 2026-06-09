package com.banking.customer_service.dto;

import com.banking.customer_service.entity.StatutKyc;

// Corps pour PATCH /api/customers/{id}/kyc  -> { "statutKyc": "VALIDE" }
public record KycRequestDTO(
        StatutKyc statutKyc
) {
}
