package com.banking.loan_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pret")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demande_id", nullable = false, unique = true)
    private Long demandeId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "compte_id", nullable = false)
    private Long compteId;

    @Column(name = "montant_accorde", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantAccorde;

    @Column(name = "taux_interet", nullable = false, precision = 5, scale = 4)
    private BigDecimal tauxInteret;

    @Column(name = "duree_mois", nullable = false)
    private int dureeMois;

    @Column(name = "capital_restant", nullable = false, precision = 15, scale = 2)
    private BigDecimal capitalRestant;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutPret statut;

    @Column(name = "date_deblocage")
    private LocalDateTime dateDeblocage;

    @OneToMany(mappedBy = "pret", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Echeance> echeances;

    @PrePersist
    protected void onCreate() {
        if (this.statut == null) {
            this.statut = StatutPret.ACTIF;
        }
        if (this.dateDeblocage == null) {
            this.dateDeblocage = LocalDateTime.now();
        }
        if (this.capitalRestant == null) {
            this.capitalRestant = this.montantAccorde;
        }
    }
}