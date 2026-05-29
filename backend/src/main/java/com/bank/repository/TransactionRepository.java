package com.bank.repository;

import com.bank.domain.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    Optional<Transaction> findByReferenceId(String referenceId);
}
