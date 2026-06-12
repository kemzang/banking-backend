package com.banking.transaction_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RetraitRequestDTO(
        @Schema(example = "1") @NotNull @Positive Long compteId,
        @Schema(example = "10000") @NotNull
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal montant,
        @Schema(example = "XAF") @NotBlank String devise
) {
}
