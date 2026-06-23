package com.banking.auth_service.repository;

import com.banking.auth_service.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import com.banking.auth_service.entity.Role;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<Utilisateur> findByEmailIgnoreCase(String email);
    List<Utilisateur> findByOperatorIdAndRolesContaining(Long operatorId, Role role);
    boolean existsByOperatorIdAndRolesContaining(Long operatorId, Role role);
}
