package com.banking.loan_service.repository;

import com.banking.loan_service.entity.Echeance;
import com.banking.loan_service.entity.StatutEcheance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EcheanceRepository extends JpaRepository<Echeance, Long> {
    
    List<Echeance> findByPretIdOrderByNumero(Long pretId);
    
    @Query("SELECT e FROM Echeance e WHERE e.pret.id = :pretId AND e.statut = :statut ORDER BY e.numero")
    Optional<Echeance> findFirstByPretIdAndStatutOrderByNumero(@Param("pretId") Long pretId, @Param("statut") StatutEcheance statut);
}