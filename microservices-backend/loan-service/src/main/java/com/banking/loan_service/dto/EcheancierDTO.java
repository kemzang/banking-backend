package com.banking.loan_service.dto;

import java.util.List;

public record EcheancierDTO(
    Long pretId,
    List<EcheanceDTO> echeances
) {}