package com.banking.loan_service.repository;

import com.banking.loan_service.entity.DemandePret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.banking.loan_service.entity.StatutDemande;

@Repository
public interface DemandePretRepository extends JpaRepository<DemandePret, Long> {
    List<DemandePret> findByClientIdOrderByDateSoumissionDesc(Long clientId);
    List<DemandePret> findByStatutOrderByDateSoumissionDesc(StatutDemande statut);
    List<DemandePret> findByOperatorIdAndStatutOrderByDateSoumissionDesc(Long operatorId, StatutDemande statut);
}
