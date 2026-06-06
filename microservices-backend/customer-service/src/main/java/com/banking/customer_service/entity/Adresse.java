package com.banking.customer_service.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// @Embeddable (et NON @Entity) : Value Object incorpore dans la table client.
// Pas de @Id : une adresse n'a pas d'identite propre, elle n'existe que dans un client.
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Adresse {
    private String rue;
    private String ville;
    private String pays;
    private String codePostal;
}
