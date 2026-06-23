package com.banking.loan_service.repository;

import com.banking.loan_service.entity.DemandePret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DemandePretRepository extends JpaRepository<DemandePret, Long> {
    List<DemandePret> findByClientIdOrderByDateSoumissionDesc(Long clientId);
}
