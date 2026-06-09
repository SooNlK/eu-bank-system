package com.bank.domain.transfer;

import com.bank.domain.account.Account;
import com.bank.domain.customer.Customer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.bank.domain.shared.Money;
import org.springframework.data.domain.AbstractAggregateRoot;

@Entity
@Table(name = "transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer extends AbstractAggregateRoot<Transfer> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    /** Wypełniony tylko dla przelewów INTERNAL – konto odbiorcy w naszym banku */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    /** IBAN zewnętrznego odbiorcy (SEPA / SEPA_INSTANT / TARGET) */
    @Column(name = "to_iban", length = 34)
    private String toIban;

    /** BIC banku zewnętrznego odbiorcy */
    @Column(name = "to_bic", length = 11)
    private String toBic;

    /** Nazwa zewnętrznego odbiorcy */
    @Column(name = "beneficiary_name", length = 255)
    private String beneficiaryName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false, precision = 19, scale = 4)),
            @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name = "external_reference_id", length = 255)
    private String externalReferenceId;

    @Column(length = 255)
    private String description;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Customer approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    // ===== Pola specyficzne dla przelewów SWIFT =====

    /** ID wiadomości pacs.008 wysłanej do symulatora SWIFT */
    @Column(name = "swift_msg_id", length = 64)
    private String swiftMsgId;

    /** UETR – Unique End-to-End Transaction Reference (UUID) */
    @Column(name = "swift_uetr", length = 36)
    private String swiftUetr;

    /** Podział opłat: SHA/OUR/BEN/CRED */
    @Column(name = "swift_charge_bearer", length = 4)
    private String swiftChargeBearer;

    /** Trasa jako JSON array: ["BANKDEXX","UKBKGB01XXX","USBKUS01XXX"] */
    @Column(name = "swift_route", columnDefinition = "TEXT")
    private String swiftRoute;

    /** Opłata SWIFT pobrana od klienta (w EUR) */
    @Column(name = "swift_fee", precision = 19, scale = 4)
    private BigDecimal swiftFee;

    /** Kurs wymiany EUR → waluta docelowa */
    @Column(name = "swift_fx_rate", precision = 19, scale = 8)
    private BigDecimal swiftFxRate;

    /** Waluta docelowa SWIFT (np. USD, GBP) */
    @Column(name = "swift_target_currency", length = 3)
    private String swiftTargetCurrency;

    /** Czy przelew był odwołany (Recall) */
    @Column(name = "swift_recalled", nullable = false)
    private boolean swiftRecalled = false;
}

