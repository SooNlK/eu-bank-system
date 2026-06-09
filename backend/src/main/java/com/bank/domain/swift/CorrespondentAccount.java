package com.bank.domain.swift;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Konto nostro naszego banku (BANKDEXX) prowadzone przez bank-korespondent.
 * Np. nasze konto GBP u UKBKGB01XXX, używane do rozliczeń SWIFT hop-to-hop.
 */
@Entity
@Table(name = "correspondent_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrespondentAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** BIC banku partnera (korespondenta), np. "UKBKGB01XXX" */
    @Column(name = "correspondent_bic", nullable = false, length = 11)
    private String correspondentBic;

    /** Nazwa banku partnera */
    @Column(name = "correspondent_name", nullable = false, length = 128)
    private String correspondentName;

    /** Numer konta naszego banku u korespondenta */
    @Column(name = "account_number", nullable = false, length = 64, unique = true)
    private String accountNumber;

    /** Waluta rachunku, np. "GBP", "USD", "PLN" */
    @Column(nullable = false, length = 3)
    private String currency;

    /** Bieżące saldo rachunku nostro */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
