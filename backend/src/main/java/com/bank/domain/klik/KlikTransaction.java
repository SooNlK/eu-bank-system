package com.bank.domain.klik;

import com.bank.domain.account.Account;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "klik_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KlikTransaction {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id; // transactionId z systemu KLIK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "merchant_name", nullable = false, length = 255)
    private String merchantName;

    @Column(nullable = false, length = 50)
    private String status; // PENDING, APPROVED, COMPLETED, REJECTED, TIMEOUT

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
