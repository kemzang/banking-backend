package com.banking.customer_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "operateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    private TypeOperateur type;

    @Column(unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutOperateur statut = StatutOperateur.ACTIVE;

    @PrePersist
    void onCreate() {
        if (statut == null) {
            statut = StatutOperateur.ACTIVE;
        }
    }
}
