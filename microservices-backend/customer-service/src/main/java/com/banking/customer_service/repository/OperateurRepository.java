package com.banking.customer_service.repository;

import com.banking.customer_service.entity.Operateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperateurRepository extends JpaRepository<Operateur, Long> {
    boolean existsByCode(String code);
}
