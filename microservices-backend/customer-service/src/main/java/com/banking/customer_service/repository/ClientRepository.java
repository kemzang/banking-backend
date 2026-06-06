package com.banking.customer_service.repository;

import com.banking.customer_service.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByEmail(String email);
    Optional<Client> findByEmail(String email);
    
}
