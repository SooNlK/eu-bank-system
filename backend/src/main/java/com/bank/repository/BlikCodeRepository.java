package com.bank.repository;

import com.bank.domain.klik.KlikCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BlikCodeRepository extends JpaRepository<KlikCode, UUID> {
    
}
