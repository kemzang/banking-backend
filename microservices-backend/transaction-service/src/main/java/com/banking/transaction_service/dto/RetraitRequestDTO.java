package com.banking.transaction_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RetraitRequestDTO(
        @NotNull @Positive Long compteId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal montant,
        @NotBlank String devise
) {
}
