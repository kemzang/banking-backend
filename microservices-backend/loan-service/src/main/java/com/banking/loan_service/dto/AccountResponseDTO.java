package com.banking.loan_service.dto;

public record AccountResponseDTO(Long id, String numeroCompte, Long clientId, Long operateurId, String statut) {}
