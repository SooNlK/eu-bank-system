package com.bank.service;

import com.bank.client.swift.Pacs008Builder;
import com.bank.client.swift.Pacs008Parser;
import com.bank.client.swift.SwiftClient;
import com.bank.config.SwiftProperties;
import com.bank.domain.account.Account;
import com.bank.domain.account.AccountStatus;
import com.bank.domain.shared.Money;
import com.bank.domain.transaction.Transaction;
import com.bank.domain.transaction.TransactionStatus;
import com.bank.domain.transaction.TransactionType;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.domain.transfer.Transfer;
import com.bank.domain.transfer.TransferStatus;
import com.bank.repository.TransferRepository;
import com.bank.repository.CorrespondentAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Obsługa przychodzących wiadomości SWIFT od symulatora.
 *
 * Gdy nasz bank jest odbiorcą przelewu SWIFT:
 * - symulator wysyła pacs.008 XML na endpoint /receive
 * - parsujemy XML, sprawdzamy konto odbiorcy
 * - jeśli konto aktywne: creditujemy i odpowiadamy 200 ACCEPTED
 * - jeśli konto nieistnieje/zamknięte: wysyłamy Recall i odpowiadamy 200 REJECTED
 */
@Service
public class SwiftIncomingService {

    private static final Logger log = LoggerFactory.getLogger(SwiftIncomingService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FxService fxService;
    private final SwiftClient swiftClient;
    private final SwiftProperties swiftProperties;
    private final TransferRepository transferRepository;
    private final CorrespondentAccountRepository correspondentAccountRepository;

    public SwiftIncomingService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            FxService fxService,
            SwiftClient swiftClient,
            SwiftProperties swiftProperties,
            TransferRepository transferRepository,
            CorrespondentAccountRepository correspondentAccountRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.fxService = fxService;
        this.swiftClient = swiftClient;
        this.swiftProperties = swiftProperties;
        this.transferRepository = transferRepository;
        this.correspondentAccountRepository = correspondentAccountRepository;
    }

    /**
     * Przetwarza przychodzący przelew SWIFT.
     * @return "ACCEPTED" lub "REJECTED" (Recall wysłany)
     */
    @Transactional
    public String processIncoming(String pacs008Xml) {
        return processIncoming(pacs008Xml, false);
    }

    @Transactional
    public String processIncoming(String pacs008Xml, boolean isReturnHeader) {
        Pacs008Parser.ParsedPacs008 parsed = Pacs008Parser.parse(pacs008Xml);
        
        boolean isReturn = isReturnHeader || 
                (parsed.remittance() != null && parsed.remittance().toLowerCase().contains("zwrot"));

        if (isReturn) {
            log.info("SWIFT /receive: Wykryto zwrot płatności (Recall/Return) dla UETR={}", parsed.uetr());
            
            Transfer originalTransfer = transferRepository.findBySwiftUetr(parsed.uetr()).orElse(null);
            if (originalTransfer == null) {
                log.warn("SWIFT Recall: Nie znaleziono oryginalnego przelewu dla UETR={}", parsed.uetr());
                return "ACCEPTED";
            }

            if (originalTransfer.isSwiftRecalled()) {
                log.warn("SWIFT Recall: Przelew dla UETR={} był już uprzednio wycofany", parsed.uetr());
                return "ACCEPTED";
            }

            Account senderAccount = originalTransfer.getFromAccount();
            if (senderAccount == null) {
                log.error("SWIFT Recall: Oryginalne konto nadawcy jest null dla UETR={}", parsed.uetr());
                return "ACCEPTED";
            }

            // Zwracamy środki na konto nadawcy (kwota + opłata)
            BigDecimal refundAmount = originalTransfer.getAmount().getAmount();
            BigDecimal feeRefund = originalTransfer.getSwiftFee() != null ? originalTransfer.getSwiftFee() : BigDecimal.ZERO;
            BigDecimal totalRefund = refundAmount.add(feeRefund);

            String accountCurrency = senderAccount.getBalance().getCurrency();
            Money refund = Money.of(totalRefund, accountCurrency);
            senderAccount.setBalance(senderAccount.getBalance().add(refund));
            accountRepository.save(senderAccount);

            // Zwracamy środki na konto nostro (jeśli było obciążone)
            BigDecimal targetAmount = parsed.amount();
            String targetCurrency = parsed.currency();

            if (originalTransfer.getSwiftRoute() != null) {
                String firstCorrespondentBic = getFirstCorrespondentFromRoute(originalTransfer.getSwiftRoute());
                if (firstCorrespondentBic != null) {
                    correspondentAccountRepository
                            .findByCorrespondentBicAndCurrencyAndStatus(firstCorrespondentBic, targetCurrency, "ACTIVE")
                            .ifPresent(nostro -> {
                                nostro.setBalance(nostro.getBalance().add(targetAmount));
                                correspondentAccountRepository.save(nostro);
                                log.info("SWIFT Recall: zwrócono na konto Nostro {} ({}) kwotę {} {}",
                                        nostro.getAccountNumber(), firstCorrespondentBic, targetAmount, targetCurrency);
                            });
                }
            }

            // Zapisz transakcję CREDIT na koncie klienta
            String desc = "Zwrot przelewu SWIFT UETR: " + parsed.uetr() + 
                    (parsed.remittance() != null && !parsed.remittance().isBlank() ? " | " + parsed.remittance() : "");
            transactionRepository.save(Transaction.builder()
                    .account(senderAccount)
                    .amount(refund)
                    .type(TransactionType.CREDIT)
                    .status(TransactionStatus.COMPLETED)
                    .description(desc)
                    .referenceId(parsed.uetr())
                    .counterpartyName(originalTransfer.getBeneficiaryName())
                    .counterpartyIban(originalTransfer.getToIban())
                    .build());

            // Aktualizuj status oryginalnego przelewu
            originalTransfer.setStatus(TransferStatus.FAILED);
            originalTransfer.setSwiftRecalled(true);
            transferRepository.save(originalTransfer);

            log.info("SWIFT Recall: Pomyślnie zwrócono {} {} na konto {} dla UETR={}",
                    totalRefund, accountCurrency, senderAccount.getAccountNumber().getValue(), parsed.uetr());

            return "ACCEPTED";
        }

        log.info("SWIFT przychodzący: msgId={}, uetr={}, receiver={}, amount={} {}",
                parsed.msgId(), parsed.uetr(), parsed.receiverAccount(),
                parsed.amount(), parsed.currency());

        // Znajdź konto odbiorcy (IBAN lub numer konta)
        Account receiverAccount = accountRepository
                .findByAccountNumber_Value(parsed.receiverAccount())
                .orElse(null);

        if (receiverAccount == null || receiverAccount.getStatus() != AccountStatus.ACTIVE) {
            log.warn("SWIFT Recall: konto {} nie istnieje lub jest nieaktywne", parsed.receiverAccount());
            sendRecall(parsed);
            return "REJECTED";
        }

        // Przeliczenie kwoty do EUR (waluta rachunku w BANKDEXX)
        String accountCurrency = receiverAccount.getBalance().getCurrency();
        BigDecimal creditAmount;
        BigDecimal fxRate = BigDecimal.ONE;

        if (parsed.currency().equalsIgnoreCase(accountCurrency)) {
            creditAmount = parsed.amount();
        } else {
            fxRate = fxService.getRate(parsed.currency(), accountCurrency);
            creditAmount = fxService.convert(parsed.amount(), parsed.currency(), accountCurrency);
        }

        // Uznanie konta
        Money credit = Money.of(creditAmount, accountCurrency);
        receiverAccount.setBalance(receiverAccount.getBalance().add(credit));
        accountRepository.save(receiverAccount);

        // Zapis transakcji CREDIT
        String desc = "SWIFT przychodzący od " + parsed.senderBic() +
                (parsed.remittance() != null && !parsed.remittance().isBlank()
                        ? " | " + parsed.remittance() : "");
        transactionRepository.save(Transaction.builder()
                .account(receiverAccount)
                .amount(credit)
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.COMPLETED)
                .description(desc)
                .referenceId(parsed.uetr())
                .counterpartyName(parsed.senderName())
                .counterpartyIban(parsed.senderIban())
                .build());

        log.info("SWIFT: zaksięgowano {} {} na konto {} (uetr={})",
                creditAmount, accountCurrency, parsed.receiverAccount(), parsed.uetr());
        return "ACCEPTED";
    }

    private String getFirstCorrespondentFromRoute(String routeJson) {
        if (routeJson == null || !routeJson.startsWith("[")) {
            return null;
        }
        try {
            String clean = routeJson.replace("[", "").replace("]", "").replace("\"", "");
            String[] parts = clean.split(",");
            if (parts.length > 1) {
                return parts[1].trim();
            }
        } catch (Exception e) {
            log.error("Błąd parsowania trasy SWIFT: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Wysyła Recall – nowy pacs.008 z odwróconymi stronami (my wysyłamy z powrotem do nadawcy).
     */
    private void sendRecall(Pacs008Parser.ParsedPacs008 original) {
        try {
            String recallMsgId = "RECALL-" + UUID.randomUUID().toString().replace("-","").substring(0, 10).toUpperCase();
            String recallUetr  = UUID.randomUUID().toString();

            String recallXml = Pacs008Builder.build(
                    recallMsgId, recallUetr, original.uetr() != null ? original.uetr() : recallUetr,
                    original.amount(), original.currency(),
                    // Odwrócenie ról: my jesteśmy teraz "nadawcą" zwrotu
                    "BANKDEXX Recall", original.receiverAccount(), swiftProperties.bankBic(),
                    // Odbiorca zwrotu = oryginalny nadawca
                    original.senderName(), original.senderIban(), original.senderBic(),
                    "SHAR",
                    "Zwrot - RECALL: Konto odbiorcy nie istnieje lub jest zamknięte. Ref: " + original.msgId(),
                    LocalDate.now()
            );

            swiftClient.sendReturn(recallXml);
            log.info("SWIFT Recall (Zwrot) wysłany: recallMsgId={}, do={}", recallMsgId, original.senderBic());
        } catch (Exception e) {
            log.error("Błąd podczas wysyłania SWIFT Recall: {}", e.getMessage(), e);
        }
    }
}
