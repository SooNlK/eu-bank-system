package com.bank.domain.card;

import com.bank.domain.account.Account;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, length = 4)
    private String last4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CardType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "external_card_token", length = 255)
    private String externalCardToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "daily_limit", precision = 19, scale = 4)
    private BigDecimal dailyLimit;

    @Column(name = "monthly_limit", precision = 19, scale = 4)
    private BigDecimal monthlyLimit;
}
