package com.banking.auth_service.repository;

import com.banking.auth_service.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {
    boolean existsByEmail(String email);
    Optional<Utilisateur> findByEmail(String email);
}
