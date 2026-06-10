package com.banking.transaction_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransfertRequestDTO(
        @NotNull @Positive Long compteSourceId,
        @NotNull @Positive Long compteDestId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal montant,
        @NotBlank String devise,
        @Size(max = 500) String motif
) {
}
