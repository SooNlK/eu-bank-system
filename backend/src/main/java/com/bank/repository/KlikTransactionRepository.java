package com.bank.repository;

import com.bank.domain.account.Account;
import com.bank.domain.klik.KlikTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface KlikTransactionRepository extends JpaRepository<KlikTransaction, UUID> {
    
    List<KlikTransaction> findByAccountInAndStatus(Collection<Account> accounts, String status);
}
