package com.banking.customer_service.dto;

import com.banking.customer_service.entity.TypePiece;
import com.banking.customer_service.entity.Adresse;
import java.util.UUID;
import java.time.LocalDate;

public record ClientRequestDTO(
    UUID utilisateurId,
    Long operateurId,
    String nom,
    String prenom,
    LocalDate dateNaissance,
    String email,
    String telephone,
    String numeroIdentite,
    TypePiece typePiece,
    Adresse adresse
) {
    
}
