package com.banking.customer_service.repository;

import com.banking.customer_service.entity.Operateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.banking.customer_service.entity.StatutOperateur;

@Repository
public interface OperateurRepository extends JpaRepository<Operateur, Long> {
    boolean existsByCode(String code);
    List<Operateur> findByStatutOrderByNomAsc(StatutOperateur statut);
}
