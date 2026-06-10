package com.banking.transaction_service.entity;

import com.banking.transaction_service.enums.StatutTransaction;
import com.banking.transaction_service.enums.TypeTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 32)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeTransaction type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false, length = 3)
    private String devise;

    private Long compteSourceId;
    private Long compteDestId;
    private Long operateurSourceId;
    private Long operateurDestId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal commission = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutTransaction statut = StatutTransaction.INITIEE;

    @Column(length = 500)
    private String motif;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateOperation;

    @PrePersist
    void prePersist() {
        if (dateOperation == null) {
            dateOperation = LocalDateTime.now();
        }
        if (statut == null) {
            statut = StatutTransaction.INITIEE;
        }
        if (commission == null) {
            commission = BigDecimal.ZERO;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public TypeTransaction getType() {
        return type;
    }

    public void setType(TypeTransaction type) {
        this.type = type;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public Long getCompteSourceId() {
        return compteSourceId;
    }

    public void setCompteSourceId(Long compteSourceId) {
        this.compteSourceId = compteSourceId;
    }

    public Long getCompteDestId() {
        return compteDestId;
    }

    public void setCompteDestId(Long compteDestId) {
        this.compteDestId = compteDestId;
    }

    public Long getOperateurSourceId() {
        return operateurSourceId;
    }

    public void setOperateurSourceId(Long operateurSourceId) {
        this.operateurSourceId = operateurSourceId;
    }

    public Long getOperateurDestId() {
        return operateurDestId;
    }

    public void setOperateurDestId(Long operateurDestId) {
        this.operateurDestId = operateurDestId;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public void setCommission(BigDecimal commission) {
        this.commission = commission;
    }

    public StatutTransaction getStatut() {
        return statut;
    }

    public void setStatut(StatutTransaction statut) {
        this.statut = statut;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public LocalDateTime getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(LocalDateTime dateOperation) {
        this.dateOperation = dateOperation;
    }
}
