package com.banking.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)   // id genere automatiquement sous forme d'UUID
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    private String nom;

    private String prenom;

    @Column(nullable = false)
    private String motDePasse;        // contient le HACHAGE BCrypt, jamais le mot de passe en clair

    private String telephone;

    @Enumerated(EnumType.STRING)
    private StatutUtilisateur statut;

    // @ElementCollection : stocke l'ensemble des roles dans une table secondaire
    // (utilisateur_roles), sans avoir besoin d'une entite Role complete.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "utilisateur_roles", joinColumns = @JoinColumn(name = "utilisateur_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // Identifiant de l'organisation dans customer-service. Il n'est pas une FK locale.
    private Long operatorId;

    private LocalDateTime dateCreation;

    @PrePersist
    void onCreate() {
        this.dateCreation = LocalDateTime.now();
        if (this.statut == null) {
            this.statut = StatutUtilisateur.ACTIF;
        }
        validateOperatorAssignment();
    }

    @PreUpdate
    void onUpdate() {
        validateOperatorAssignment();
    }

    private void validateOperatorAssignment() {
        boolean operatorUser = roles != null && roles.stream().anyMatch(Role::isOperatorRole);
        if (operatorUser && (operatorId == null || operatorId <= 0)) {
            throw new IllegalStateException("Un utilisateur operateur doit avoir un operatorId positif");
        }
    }
}
