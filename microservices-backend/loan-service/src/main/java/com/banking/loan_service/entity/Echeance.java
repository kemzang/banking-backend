package com.banking.loan_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "echeance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Echeance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pret_id", nullable = false)
    private Pret pret;

    @Column(name = "numero", nullable = false)
    private int numero;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @Column(name = "montant_capital", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantCapital;

    @Column(name = "montant_interet", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantInteret;

    @Column(name = "montant_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutEcheance statut;

    @PrePersist
    protected void onCreate() {
        if (this.statut == null) {
            this.statut = StatutEcheance.A_PAYER;
        }
    }
}