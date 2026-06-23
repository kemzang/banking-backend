package com.banking.loan_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_pret")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandePret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "montant_demande", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantDemande;

    @Column(name = "duree_mois", nullable = false)
    private int dureeMois;

    @Column(name = "motif")
    private String motif;

    @Column(name = "score_risque", precision = 5, scale = 2)
    private BigDecimal scoreRisque;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutDemande statut;

    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;

    @PrePersist
    protected void onCreate() {
        if (this.statut == null) {
            this.statut = StatutDemande.SOUMISE;
        }
        if (this.dateSoumission == null) {
            this.dateSoumission = LocalDateTime.now();
        }
    }
}
