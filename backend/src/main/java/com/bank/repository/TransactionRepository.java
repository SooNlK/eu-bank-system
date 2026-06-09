package com.bank.repository;

import com.bank.domain.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    Optional<Transaction> findByReferenceId(String referenceId);

    @Query("SELECT COALESCE(SUM(t.amount.amount), 0) FROM Transaction t " +
           "WHERE t.card.id = :cardId " +
           "AND t.type = com.bank.domain.transaction.TransactionType.DEBIT " +
           "AND t.status = com.bank.domain.transaction.TransactionStatus.COMPLETED " +
           "AND t.createdAt >= :startDate")
    BigDecimal sumAmountByCardIdAndTypeAndStatusAndCreatedAtAfter(
            @Param("cardId") UUID cardId,
            @Param("startDate") LocalDateTime startDate);
}
