package com.banking.account_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banking.account_service.entity.Compte;

public interface CompteRepository extends JpaRepository<Compte, Long> {

    List<Compte> findByClientId(Long clientId);

    Optional<Compte> findByNumeroCompte(String numeroCompte);

    boolean existsByNumeroCompte(String numeroCompte);

    long countByOperateurId(Long operateurId);
}
