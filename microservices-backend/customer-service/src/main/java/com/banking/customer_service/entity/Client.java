package com.banking.customer_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "client")
@Getter
@Setter
@NoArgsConstructor      // OBLIGATOIRE pour JPA (instanciation par reflexion)
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // References vers d'autres services : de simples ID, pas de @ManyToOne
    private UUID utilisateurId;
    private Long operateurId;

    @Column(nullable = false)
    private String nom;
    @Column(nullable = false)
    private String prenom;
    @Column(nullable = false)
    private LocalDate dateNaissance;

    @Column(nullable = false, unique = true)
    private String email;
    private String telephone;
    private String numeroIdentite;

    @Enumerated(EnumType.STRING)
    private TypePiece typePiece;

    @Enumerated(EnumType.STRING)
    private StatutKyc statutKyc;

    private String motifRejet;

    @Embedded
    private Adresse adresse;

    private LocalDateTime dateInscription;

    @PrePersist
    void onCreate() {
        this.dateInscription = LocalDateTime.now();
        if (this.statutKyc == null) {
            this.statutKyc = StatutKyc.EN_ATTENTE;
        }
    }
}
