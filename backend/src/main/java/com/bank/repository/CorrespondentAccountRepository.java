package com.bank.repository;

import com.bank.domain.swift.CorrespondentAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorrespondentAccountRepository extends JpaRepository<CorrespondentAccount, UUID> {

    /** Znajdź konto nostro u danego korespondenta w danej walucie */
    Optional<CorrespondentAccount> findByCorrespondentBicAndCurrencyAndStatus(
            String correspondentBic, String currency, String status);

    /** Wszystkie aktywne konta nostro w danej walucie (fallback routing) */
    List<CorrespondentAccount> findByCurrencyAndStatus(String currency, String status);

    /** Sprawdź czy mamy jakiekolwiek konto nostro w danej walucie */
    boolean existsByCurrencyAndStatus(String currency, String status);

    /** Wszystkie konta nostro u danego korespondenta o określonym statusie */
    List<CorrespondentAccount> findByCorrespondentBicAndStatus(String correspondentBic, String status);
}
