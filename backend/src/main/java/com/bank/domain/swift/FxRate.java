package com.bank.domain.swift;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kurs wymiany walut banku. EUR jest walutą bazową BANKDEXX.
 * Kursy są sztywne (wewnętrzne stawki banku).
 */
@Entity
@Table(name = "fx_rates",
       uniqueConstraints = @UniqueConstraint(columnNames = {"from_currency", "to_currency"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FxRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    /** Ile jednostek toCurrency za 1 jednostkę fromCurrency */
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;
}
