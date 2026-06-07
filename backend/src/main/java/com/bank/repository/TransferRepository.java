package com.bank.repository;

import com.bank.domain.transfer.Transfer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.bank.domain.transfer.TransferChannel;
import com.bank.domain.transfer.TransferStatus;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    List<Transfer> findByChannelAndStatus(TransferChannel channel, TransferStatus status);

    @Query("""
            select t from Transfer t
            where t.fromAccount.customer.email = :email
               or t.toAccount.customer.email = :email
            order by t.createdAt desc
            """)
    List<Transfer> findHistoryForCustomer(@Param("email") String email);

    @Query("""
            select t from Transfer t
            where t.id = :id
              and (t.fromAccount.customer.email = :email or t.toAccount.customer.email = :email)
            """)
    Optional<Transfer> findVisibleToCustomer(@Param("id") UUID id, @Param("email") String email);
}
