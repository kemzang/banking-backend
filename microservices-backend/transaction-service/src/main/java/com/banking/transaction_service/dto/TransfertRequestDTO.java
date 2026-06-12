package com.banking.transaction_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransfertRequestDTO(
        @Schema(example = "1") @NotNull @Positive Long compteSourceId,
        @Schema(example = "2") @NotNull @Positive Long compteDestId,
        @Schema(example = "25000") @NotNull
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal montant,
        @Schema(example = "XAF") @NotBlank String devise,
        @Schema(example = "Paiement fournisseur") @Size(max = 500) String motif
) {
}
