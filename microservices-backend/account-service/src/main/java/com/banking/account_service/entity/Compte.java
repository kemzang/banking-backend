package com.banking.account_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "compte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Références vers d'autres services : simples ID, pas de @ManyToOne
    private Long clientId;
    private Long operateurId;

    @Column(nullable = false, unique = true)
    private String numeroCompte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCompte type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal solde;

    @Column(nullable = false, length = 3)
    private String devise;

    // Plafond de retrait journalier (null = pas de plafond)
    @Column(precision = 15, scale = 2)
    private BigDecimal plafondJournalier;

    // Découvert autorisé (valeur négative minimale du solde)
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal decouvertAutorise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCompte statut;

    private LocalDateTime dateOuverture;
    private LocalDateTime dateCloture;

    @PrePersist
    void onCreate() {
        this.dateOuverture = LocalDateTime.now();
        if (this.statut == null)       this.statut = StatutCompte.EN_ATTENTE_ACTIVATION;
        if (this.solde == null)        this.solde = BigDecimal.ZERO;
        if (this.decouvertAutorise == null) this.decouvertAutorise = BigDecimal.ZERO;
        if (this.devise == null)       this.devise = "XAF";
    }
}
