package com.banking.loan_service.repository;

import com.banking.loan_service.entity.Pret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PretRepository extends JpaRepository<Pret, Long> {
    List<Pret> findByClientIdOrderByDateDeblocageDesc(Long clientId);
}
