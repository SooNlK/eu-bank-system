package com.bank.service;

import com.bank.client.klik.KlikClient;
import com.bank.domain.account.Account;
import com.bank.domain.customer.Customer;
import com.bank.domain.klik.KlikCode;
import com.bank.domain.klik.KlikTransaction;
import com.bank.domain.shared.Money;
import com.bank.domain.transaction.Transaction;
import com.bank.domain.transaction.TransactionStatus;
import com.bank.domain.transaction.TransactionType;
import com.bank.dto.blik.BlikGenerateRequest;
import com.bank.dto.blik.BlikGenerateResponse;
import com.bank.api.KlikWebhookController;
import com.bank.repository.AccountRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.KlikCodeRepository;
import com.bank.repository.KlikTransactionRepository;
import com.bank.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class KlikService {

    private static final Logger log = LoggerFactory.getLogger(KlikService.class);

    private final KlikClient klikClient;
    private final KlikCodeRepository klikCodeRepository;
    private final KlikTransactionRepository klikTransactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final AccountService accountService;

    public KlikService(
            KlikClient klikClient,
            KlikCodeRepository klikCodeRepository,
            KlikTransactionRepository klikTransactionRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            CustomerRepository customerRepository,
            AccountService accountService
    ) {
        this.klikClient = klikClient;
        this.klikCodeRepository = klikCodeRepository;
        this.klikTransactionRepository = klikTransactionRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
        this.accountService = accountService;
    }

    /**
     * Generuje jednorazowy kod KLIK powiązany z danym rachunkiem.
     */
    @Transactional
    public BlikGenerateResponse generateCode(BlikGenerateRequest request, String customerEmail) {
        // 1. Walidacja dostępu do konta (obsługuje również konta JUNIOR przez rodzica)
        Account account;
        try {
            account = accountService.getAccountEntity(request.accountId(), customerEmail);
        } catch (RuntimeException e) {
            if ("Access denied".equals(e.getMessage())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak dostępu do podanego rachunku.");
            } else if ("Account not found".equals(e.getMessage())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rachunek nie istnieje.");
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }

        // 2. Wywołanie zewnętrznego API KLIK
        KlikClient.CodeGenerateResponse klikResp = klikClient.generateCode(account.getId().toString());

        // 3. Zahashowanie kodu (zgodnie ze schematem bazy danych)
        String codeHash = sha256(klikResp.code());

        // 4. Parsowanie daty ważności
        LocalDateTime expiresAt;
        try {
            expiresAt = ZonedDateTime.parse(klikResp.expiresAt()).toLocalDateTime();
        } catch (Exception e) {
            expiresAt = LocalDateTime.now().plusSeconds(klikResp.expiresIn());
        }

        // 5. Zapis w lokalnej bazie danych
        KlikCode codeEntity = KlikCode.builder()
                .account(account)
                .codeHash(codeHash)
                .expiresAt(expiresAt)
                .used(false)
                .build();
        codeEntity = klikCodeRepository.save(codeEntity);

        return new BlikGenerateResponse(
                codeEntity.getId(),
                klikResp.code(),
                expiresAt
        );
    }

    /**
     * Obsługuje webhook autoryzacyjny przychodzący z KLIK (zapisuje transakcję jako PENDING).
     */
    @Transactional
    public KlikWebhookController.AuthorizeResponse authorizeWebhook(KlikWebhookController.AuthorizeRequest request) {
        UUID accountId;
        try {
            accountId = UUID.fromString(request.userId());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawny format identyfikatora użytkownika (oczekiwano UUID).");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rachunek odbiorcy kodu nie został odnaleziony."));

        // Zapis w bazie banku w statusie oczekującym (PENDING)
        KlikTransaction klikTx = KlikTransaction.builder()
                .id(UUID.fromString(request.transactionId()))
                .account(account)
                .amount(BigDecimal.valueOf(request.amount()))
                .currency(request.currency())
                .merchantName(request.merchantName())
                .status("PENDING")
                .build();

        klikTransactionRepository.save(klikTx);

        return new KlikWebhookController.AuthorizeResponse(true, true);
    }

    /**
     * Pobiera oczekujące autoryzacje dla zalogowanego klienta.
     */
    public List<KlikTransaction> getPendingTransactions(String customerEmail) {
        Customer customer = customerRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Klient nie został znaleziony."));

        List<Account> accounts = accountRepository.findByCustomerId(customer.getId());
        return klikTransactionRepository.findByAccountInAndStatus(accounts, "PENDING");
    }

    /**
     * Procesuje decyzję użytkownika (zatwierdzenie lub odrzucenie transakcji).
     */
    @Transactional
    public void confirmTransaction(UUID transactionId, String status, String customerEmail) {
        KlikTransaction klikTx = klikTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transakcja nie istnieje."));

        Customer customer = customerRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Klient nie został znaleziony."));

        Account account = klikTx.getAccount();
        
        // Zgoda na zatwierdzenie transakcji dziecka (JUNIOR) przez rodzica
        boolean isParentOfChild = account.getType() == com.bank.domain.account.AccountType.JUNIOR &&
                account.getParentAccount() != null &&
                account.getParentAccount().getCustomer().getId().equals(customer.getId());

        if (!account.getCustomer().getId().equals(customer.getId()) && !isParentOfChild) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak dostępu do podanej transakcji.");
        }

        if (!"PENDING".equals(klikTx.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transakcja została już przetworzona.");
        }

        if ("ACCEPTED".equals(status)) {
            // 1. Sprawdzenie dostępnego salda (saldo główne minus aktualne blokady)
            BigDecimal available = account.getBalance().getAmount().subtract(account.getReservedBalance().getAmount());
            if (available.compareTo(klikTx.getAmount()) < 0) {
                klikTx.setStatus("REJECTED");
                klikTx.setRejectReason("INSUFFICIENT_FUNDS");
                klikTransactionRepository.save(klikTx);

                // Powiadomienie KLIK o odrzuceniu z braku środków
                klikClient.confirmPayment(transactionId.toString(), "REJECTED", "INSUFFICIENT_FUNDS");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niewystarczające środki na rachunku.");
            }

            // 2. Blokada środków
            account.setReservedBalance(account.getReservedBalance().add(Money.of(klikTx.getAmount(), klikTx.getCurrency())));
            accountRepository.save(account);

            klikTx.setStatus("APPROVED");
            klikTransactionRepository.save(klikTx);

            // 3. Wysłanie potwierdzenia do KLIK
            try {
                KlikClient.PaymentConfirmResponse klikResp = klikClient.confirmPayment(
                        transactionId.toString(), "ACCEPTED", null
                );

                if ("COMPLETED".equals(klikResp.status())) {
                    // Księgowanie: pobranie z salda głównego i zdjęcie blokady
                    account.setBalance(account.getBalance().subtract(Money.of(klikTx.getAmount(), klikTx.getCurrency())));
                    account.setReservedBalance(account.getReservedBalance().subtract(Money.of(klikTx.getAmount(), klikTx.getCurrency())));
                    accountRepository.save(account);

                    klikTx.setStatus("COMPLETED");
                    klikTransactionRepository.save(klikTx);

                    // Zapis w historii konta (Standardowa transakcja bankowa)
                    Transaction txn = Transaction.builder()
                            .account(account)
                            .amount(Money.of(klikTx.getAmount(), klikTx.getCurrency()))
                            .type(TransactionType.DEBIT)
                            .status(TransactionStatus.COMPLETED)
                            .description("Płatność KLIK - " + klikTx.getMerchantName())
                            .referenceId("KLIK_C2B:" + transactionId)
                            .counterpartyName(klikTx.getMerchantName())
                            .build();
                    transactionRepository.save(txn);
                    log.info("C2B transaction completed successfully: transactionId={}, amount={}", transactionId, klikTx.getAmount());
                } else {
                    // KLIK odrzucił transakcję na finiszu
                    account.setReservedBalance(account.getReservedBalance().subtract(Money.of(klikTx.getAmount(), klikTx.getCurrency())));
                    accountRepository.save(account);

                    klikTx.setStatus("REJECTED");
                    klikTx.setRejectReason(klikResp.rejectReason() != null ? klikResp.rejectReason() : "OTHER");
                    klikTransactionRepository.save(klikTx);
                    log.warn("C2B transaction was rejected by KLIK: transactionId={}", transactionId);
                }
            } catch (Exception e) {
                // Zwolnienie blokady przy błędzie komunikacji
                log.error("Error communicating with KLIK server: {}", e.getMessage(), e);
                account.setReservedBalance(account.getReservedBalance().subtract(Money.of(klikTx.getAmount(), klikTx.getCurrency())));
                accountRepository.save(account);

                klikTx.setStatus("REJECTED");
                klikTx.setRejectReason("OTHER");
                klikTransactionRepository.save(klikTx);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Błąd komunikacji z KLIK: " + e.getMessage());
            }

        } else if ("REJECTED".equals(status)) {
            klikTx.setStatus("REJECTED");
            klikTx.setRejectReason("USER_DECLINED");
            klikTransactionRepository.save(klikTx);

            klikClient.confirmPayment(transactionId.toString(), "REJECTED", "USER_DECLINED");
            log.info("C2B transaction declined by user: transactionId={}", transactionId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawny status decyzji (oczekiwano ACCEPTED lub REJECTED).");
        }
    }

    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash code with SHA-256", e);
        }
    }
}
