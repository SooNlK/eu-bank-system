package com.bank.service;

import com.bank.client.eupayments.IsoXmlBuilder;
import com.bank.client.eupayments.SepaBatchClient;
import com.bank.client.eupayments.SepaInstantClient;
import com.bank.client.swift.Pacs008Builder;
import com.bank.client.swift.SwiftClient;
import com.bank.config.EuPaymentsProperties;
import com.bank.config.SwiftProperties;
import com.bank.domain.account.Account;
import com.bank.domain.account.AccountStatus;
import com.bank.domain.customer.Customer;
import com.bank.domain.shared.IBAN;
import com.bank.domain.shared.Money;
import com.bank.domain.swift.CorrespondentAccount;
import com.bank.domain.transaction.Transaction;
import com.bank.domain.transaction.TransactionStatus;
import com.bank.domain.transaction.TransactionType;
import com.bank.domain.transfer.Transfer;
import com.bank.domain.transfer.TransferChannel;
import com.bank.domain.transfer.TransferStatus;
import com.bank.dto.transfer.TransferRequest;
import com.bank.dto.transfer.TransferResponse;
import com.bank.repository.AccountRepository;
import com.bank.repository.CorrespondentAccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private static final BigDecimal APPROVAL_LIMIT = new BigDecimal("15000.00");
    private static final BigDecimal SEPA_INSTANT_LIMIT = new BigDecimal("100000.00");
    private static final Set<String> SWIFT_CURRENCIES = Set.of("EUR", "USD", "GBP", "PLN", "CHF");

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final com.bank.repository.CustomerRepository customerRepository;
    private final SepaInstantClient sepaInstantClient;
    private final SepaBatchClient sepaBatchClient;
    private final EuPaymentsProperties euPaymentsProps;
    private final SwiftClient swiftClient;
    private final SwiftProperties swiftProperties;
    private final FxService fxService;
    private final CorrespondentAccountRepository correspondentAccountRepository;

    public TransferService(
            TransferRepository transferRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AccountService accountService,
            com.bank.repository.CustomerRepository customerRepository,
            SepaInstantClient sepaInstantClient,
            SepaBatchClient sepaBatchClient,
            EuPaymentsProperties euPaymentsProps,
            SwiftClient swiftClient,
            SwiftProperties swiftProperties,
            FxService fxService,
            CorrespondentAccountRepository correspondentAccountRepository
    ) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.customerRepository = customerRepository;
        this.sepaInstantClient = sepaInstantClient;
        this.sepaBatchClient = sepaBatchClient;
        this.euPaymentsProps = euPaymentsProps;
        this.swiftClient = swiftClient;
        this.swiftProperties = swiftProperties;
        this.fxService = fxService;
        this.correspondentAccountRepository = correspondentAccountRepository;
    }

    @Transactional
    public TransferResponse execute(TransferRequest request, String email) {
        Account fromAccount = accountService.getAccountEntity(request.fromAccountId(), email);
        LocalDate valueDate = validateValueDate(request.valueDate(), request.channel());
        String currency = validateCurrency(request.currency());
        TransferChannel channel = request.channel();

        // SWIFT ma własną ścieżkę (brak walidacji IBAN, własny FX)
        if (channel == TransferChannel.SWIFT) {
            return executeSwiftTransfer(request, fromAccount, currency, valueDate, email);
        }

        String targetIban = normalizeIban(request.toIban());

        boolean isExternal = channel == TransferChannel.SEPA
                || channel == TransferChannel.SEPA_INSTANT
                || channel == TransferChannel.TARGET;

        if (isExternal) {
            return executeExternalTransfer(request, fromAccount, targetIban, currency, valueDate);
        } else {
            return executeInternalTransfer(request, fromAccount, targetIban, currency, valueDate, email);
        }
    }

    // -----------------------------------------------------------------------
    // Przelew SWIFT – kanał międzynarodowy przez symulator SWIFT
    // -----------------------------------------------------------------------

    private TransferResponse executeSwiftTransfer(
            TransferRequest request,
            Account fromAccount,
            String currency,
            LocalDate valueDate,
            String email
    ) {
        validateSwiftTransfer(fromAccount, request);

        // Waluta docelowa SWIFT (może różnić się od EUR)
        String targetCurrency = (request.swiftTargetCurrency() != null && !request.swiftTargetCurrency().isBlank())
                ? request.swiftTargetCurrency().trim().toUpperCase()
                : currency;  // domyślnie waluta rachunku

        // Przeliczenie kwoty EUR → waluta docelowa
        BigDecimal swiftAmount = fxService.convert(request.amount(), currency, targetCurrency);
        BigDecimal fxRate = fxService.getRate(currency, targetCurrency);

        // Obliczenie opłaty SWIFT (1% od kwoty EUR, wg konfiguracji)
        String chargeBearer = (request.chargeBearer() != null && !request.chargeBearer().isBlank())
                ? request.chargeBearer().trim().toUpperCase() : "SHAR";
        BigDecimal fee = BigDecimal.ZERO;
        if ("DEBT".equals(chargeBearer) || "SHAR".equals(chargeBearer)) {
            fee = request.amount()
                    .multiply(BigDecimal.valueOf(swiftProperties.feePercent()))
                    .setScale(4, RoundingMode.HALF_UP);
        }

        // Sprawdzenie salda: kwota + opłata
        BigDecimal totalDebit = request.amount().add(fee);
        if (fromAccount.getBalance().getAmount().compareTo(totalDebit) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Niewystarczające środki. Potrzeba " + totalDebit + " EUR (kwota " +
                    request.amount() + " + opłata SWIFT " + fee + " EUR)");
        }

        // Budowanie pacs.008
        String uetr  = UUID.randomUUID().toString();
        String msgId = "MSG-" + UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase();
        String debtorIban = fromAccount.getAccountNumber().getValue();
        String pacs008 = Pacs008Builder.build(
                msgId, uetr, UUID.randomUUID().toString(),
                swiftAmount, targetCurrency,
                fromAccount.getCustomer().getFirstName() + " " + fromAccount.getCustomer().getLastName(),
                debtorIban,
                swiftProperties.bankBic(),
                request.beneficiaryName() != null ? request.beneficiaryName() : "Beneficiary",
                request.toIban(),
                request.toBic(),
                chargeBearer,
                request.description(),
                valueDate
        );

        // Wysyłka do symulatora SWIFT
        SwiftClient.SwiftPaymentResponse swiftResp = swiftClient.sendMessage(pacs008);

        // Obciążenie konta klienta (kwota + opłata)
        fromAccount.setBalance(fromAccount.getBalance().subtract(Money.of(totalDebit, currency)));
        accountRepository.save(fromAccount);

        // Obciążenie rachunku nostro (pierwszego korespondenta)
        String route = swiftResp != null && swiftResp.route() != null
                ? swiftResp.route().toString() : "[\"" + swiftProperties.bankBic() + "\",\"" + request.toBic() + "\"]"; 
        if (swiftResp != null && swiftResp.route() != null && swiftResp.route().size() > 1) {
            String firstCorrespondentBic = swiftResp.route().get(1);
            correspondentAccountRepository
                    .findByCorrespondentBicAndCurrencyAndStatus(firstCorrespondentBic, targetCurrency, "ACTIVE")
                    .ifPresent(nostro -> {
                        nostro.setBalance(nostro.getBalance().subtract(swiftAmount));
                        correspondentAccountRepository.save(nostro);
                        log.info("SWIFT nostro: obciążono konto {} ({}) kwotą {} {}",
                                nostro.getAccountNumber(), firstCorrespondentBic, swiftAmount, targetCurrency);
                    });
        }

        // Zapis transakcji DEBIT
        String swiftDesc = "SWIFT " + chargeBearer + " → " + request.toBic() +
                (request.description() != null ? " | " + request.description() : "");
        transactionRepository.save(Transaction.builder()
                .account(fromAccount)
                .amount(Money.of(totalDebit, currency))
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .description(swiftDesc)
                .referenceId(uetr)
                .counterpartyName(request.beneficiaryName())
                .counterpartyIban(request.toIban())
                .build());

        // Budowanie encji Transfer
        boolean accepted = swiftResp == null || !
                "REJECTED".equalsIgnoreCase(swiftResp.status());
        Transfer transfer = Transfer.builder()
                .fromAccount(fromAccount)
                .toAccount(null)
                .toIban(request.toIban())
                .toBic(request.toBic())
                .beneficiaryName(request.beneficiaryName())
                .amount(Money.of(request.amount(), currency))
                .channel(TransferChannel.SWIFT)
                .status(accepted ? TransferStatus.COMPLETED : TransferStatus.FAILED)
                .description(request.description())
                .valueDate(valueDate)
                .requiresApproval(false)
                .swiftMsgId(msgId)
                .swiftUetr(uetr)
                .swiftChargeBearer(chargeBearer)
                .swiftRoute(route)
                .swiftFee(fee)
                .swiftFxRate(fxRate)
                .swiftTargetCurrency(targetCurrency)
                .swiftRecalled(false)
                .completedAt(accepted ? LocalDateTime.now() : null)
                .build();

        transfer = transferRepository.save(transfer);
        log.info("SWIFT transfer {} zapisany: status={}, route={}, fee={} EUR, fxRate={}",
                transfer.getId(), transfer.getStatus(), route, fee, fxRate);
        return mapToResponse(transfer);
    }

    private void validateSwiftTransfer(Account fromAccount, TransferRequest request) {
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rachunek musi być aktywny");
        }
        if (request.toBic() == null || request.toBic().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Przelew SWIFT wymaga podania BIC banku odbiorcy (SWIFT BIC)");
        }
        if (request.toIban() == null || request.toIban().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Przelew SWIFT wymaga podania numeru rachunku odbiorcy");
        }
        if (request.beneficiaryName() == null || request.beneficiaryName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Przelew SWIFT wymaga podania nazwy odbiorcy");
        }
        if (!swiftClient.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "System SWIFT jest chwilowo niedostępny");
        }
        String targetCcy = request.swiftTargetCurrency();
        if (targetCcy != null && !targetCcy.isBlank() && !SWIFT_CURRENCIES.contains(targetCcy.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nieobsługiwana waluta SWIFT: " + targetCcy + ". Dostępne: EUR, USD, GBP, PLN, CHF");
        }
    }

    // -----------------------------------------------------------------------
    // Przelew wewnętrzny (pomiędzy kontami w naszym banku)
    // -----------------------------------------------------------------------

    private TransferResponse executeInternalTransfer(
            TransferRequest request,
            Account fromAccount,
            String targetIban,
            String currency,
            LocalDate valueDate,
            String email
    ) {
        Account toAccount = accountRepository.findByAccountNumber_Value(targetIban)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Rachunek odbiorcy nie istnieje w tym banku"));

        boolean requiresApproval = (request.amount().compareTo(APPROVAL_LIMIT) > 0) ||
                                   (fromAccount.getType() == com.bank.domain.account.AccountType.JUNIOR);

        Transfer transfer = Transfer.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(Money.of(request.amount(), currency))
                .channel(request.channel())
                .status(TransferStatus.PENDING)
                .description(request.description())
                .valueDate(valueDate)
                .requiresApproval(requiresApproval)
                .build();

        validateInternalTransfer(transfer);
        transfer = transferRepository.save(transfer);

        if (transfer.isRequiresApproval()) {
            transfer.setStatus(TransferStatus.PENDING_APPROVAL);
            transfer = transferRepository.save(transfer);
            return mapToResponse(transfer);
        }

        processInternalTransfer(transfer);
        return mapToResponse(transfer);
    }

    // -----------------------------------------------------------------------
    // Przelew zewnętrzny (SEPA, SEPA Instant, TARGET)
    // -----------------------------------------------------------------------

    private TransferResponse executeExternalTransfer(
            TransferRequest request,
            Account fromAccount,
            String targetIban,
            String currency,
            LocalDate valueDate
    ) {
        validateExternalTransfer(fromAccount, request);

        boolean requiresApproval = (request.amount().compareTo(APPROVAL_LIMIT) > 0) ||
                                   (fromAccount.getType() == com.bank.domain.account.AccountType.JUNIOR);

        Transfer transfer = Transfer.builder()
                .fromAccount(fromAccount)
                .toAccount(null) // zewnętrzny – brak konta lokalnego
                .toIban(targetIban)
                .toBic(request.toBic())
                .beneficiaryName(request.beneficiaryName())
                .amount(Money.of(request.amount(), currency))
                .channel(request.channel())
                .status(TransferStatus.PENDING)
                .description(request.description())
                .valueDate(valueDate)
                .requiresApproval(requiresApproval)
                .build();

        transfer = transferRepository.save(transfer);

        if (transfer.isRequiresApproval()) {
            transfer.setStatus(TransferStatus.PENDING_APPROVAL);
            transfer = transferRepository.save(transfer);
            return mapToResponse(transfer);
        }

        processExternalTransfer(transfer);
        return mapToResponse(transfer);
    }

    private void processExternalTransfer(Transfer transfer) {
        transfer.setStatus(TransferStatus.PROCESSING);
        transferRepository.save(transfer);

        try {
            Account fromAccount = transfer.getFromAccount();
            Money amount = transfer.getAmount();

            // Sprawdzenie salda lokalnego konta
            if (fromAccount.getBalance().getAmount().compareTo(amount.getAmount()) < 0) {
                transfer.setStatus(TransferStatus.FAILED);
                transferRepository.save(transfer);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niewystarczające środki na rachunku");
            }

            // Zbudowanie XML ISO 20022
            String endToEndId = transfer.getId().toString();
            String debtorIban = fromAccount.getAccountNumber().getValue();
            String xml = IsoXmlBuilder.build(
                    endToEndId,
                    amount.getAmount(),
                    amount.getCurrency(),
                    debtorIban,
                    transfer.getToIban(),
                    euPaymentsProps.bankBic(),
                    transfer.getToBic(),
                    transfer.getDescription()
            );

            // Routing do odpowiedniego symulatora
            String externalRef = null;
            String externalStatus = null;
            switch (transfer.getChannel()) {
                case SEPA_INSTANT -> {
                    SepaInstantClient.SepaPaymentResponse resp = sepaInstantClient.submitTransfer(xml);
                    externalRef = resp != null ? resp.endToEndId() : null;
                    externalStatus = resp != null ? resp.status() : null;
                    log.info("SEPA Instant: transfer {} → status={}, TxSts={}",
                            transfer.getId(), externalStatus, resp != null ? resp.rawTxSts() : null);
                }
                case SEPA -> {
                    SepaBatchClient.SepaBatchResponse resp = sepaBatchClient.submitTransfer(xml);
                    externalRef = resp != null ? resp.endToEndId() : null;
                    externalStatus = resp != null ? resp.status() : null;
                    log.info("SEPA Batch: transfer {} → status={}, sessionId={}",
                            transfer.getId(), externalStatus, resp != null ? resp.sessionId() : null);
                }
                case TARGET -> {
                    SepaInstantClient.SepaPaymentResponse resp = sepaInstantClient.submitTransfer(xml);
                    externalRef = resp != null ? resp.endToEndId() : null;
                    externalStatus = resp != null ? resp.status() : null;
                    log.info("TARGET: transfer {} → status={}, TxSts={}",
                            transfer.getId(), externalStatus, resp != null ? resp.rawTxSts() : null);
                }
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Nieobsługiwany kanał zewnętrzny: " + transfer.getChannel());
            }

            transfer.setExternalReferenceId(externalRef);

            // Obciążamy lokalne konto nadawcy
            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            accountRepository.save(fromAccount);

            // Zapis transakcji DEBIT
            transactionRepository.save(Transaction.builder()
                    .account(fromAccount)
                    .amount(amount)
                    .type(TransactionType.DEBIT)
                    .status(TransactionStatus.COMPLETED)
                    .description(transfer.getDescription())
                    .referenceId(transfer.getId().toString())
                    .counterpartyName(transfer.getBeneficiaryName())
                    .counterpartyIban(transfer.getToIban())
                    .build());

            // Status: dla SEPA Batch przelew jest w kolejce (PROCESSING), dla Instant/TARGET – COMPLETED
            boolean isQueued = transfer.getChannel() == TransferChannel.SEPA
                    && ("QUEUED".equalsIgnoreCase(externalStatus) || "PROCESSING".equalsIgnoreCase(externalStatus));
            boolean isFailed = "FAILED".equalsIgnoreCase(externalStatus);

            if (isFailed) {
                transfer.setStatus(TransferStatus.FAILED);
            } else {
                transfer.setStatus(isQueued ? TransferStatus.PROCESSING : TransferStatus.COMPLETED);
                if (!isQueued) {
                    transfer.setCompletedAt(LocalDateTime.now());
                }
            }

        } catch (ResponseStatusException ex) {
            transfer.setStatus(TransferStatus.FAILED);
            transferRepository.save(transfer);
            throw ex;
        } catch (Exception ex) {
            log.error("Błąd podczas przetwarzania przelewu zewnętrznego {}: {}", transfer.getId(), ex.getMessage(), ex);
            transfer.setStatus(TransferStatus.FAILED);
            transferRepository.save(transfer);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Błąd komunikacji z systemem płatniczym: " + ex.getMessage());
        }

        transferRepository.save(transfer);
    }

    // -----------------------------------------------------------------------
    // Operacje zatwierdzania / odrzucania (wspólne dla internal i external)
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TransferResponse> getHistory(String email) {
        return transferRepository.findHistoryForCustomer(email).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransferResponse getById(UUID transferId, String email) {
        return transferRepository.findVisibleToCustomer(transferId, email)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono przelewu"));
    }

    @Transactional
    public TransferResponse approve(UUID transferId, String email) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono przelewu"));

        if (transfer.getStatus() != TransferStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Przelew nie oczekuje na zatwierdzenie");
        }

        Customer currentUser = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono użytkownika"));

        if (transfer.getFromAccount().getType() == com.bank.domain.account.AccountType.JUNIOR) {
            Account juniorAccount = transfer.getFromAccount();
            Account parentAccount = juniorAccount.getParentAccount();
            if (parentAccount == null || !parentAccount.getCustomer().getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień do zatwierdzenia tego przelewu");
            }
            transfer.setApprovedBy(currentUser);
        }

        transfer.setApprovedAt(LocalDateTime.now());

        boolean isExternal = transfer.getChannel() == TransferChannel.SEPA
                || transfer.getChannel() == TransferChannel.SEPA_INSTANT
                || transfer.getChannel() == TransferChannel.TARGET;

        // SWIFT nie przechodzi przez approve flow (requiresApproval=false)
        if (isExternal) {
            processExternalTransfer(transfer);
        } else {
            processInternalTransfer(transfer);
        }

        return mapToResponse(transfer);
    }

    @Transactional
    public TransferResponse reject(UUID transferId, String email) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono przelewu"));

        if (transfer.getStatus() != TransferStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Przelew nie oczekuje na zatwierdzenie");
        }

        Customer currentUser = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono użytkownika"));

        if (transfer.getFromAccount().getType() == com.bank.domain.account.AccountType.JUNIOR) {
            Account juniorAccount = transfer.getFromAccount();
            Account parentAccount = juniorAccount.getParentAccount();
            if (parentAccount == null || !parentAccount.getCustomer().getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień do odrzucenia tego przelewu");
            }
        }

        transfer.setStatus(TransferStatus.REJECTED);
        transfer.setRejectedAt(LocalDateTime.now());
        return mapToResponse(transfer);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getPendingApprovalsForParent(String parentEmail) {
        Customer parent = customerRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono użytkownika"));

        return transferRepository.findAll().stream()
                .filter(t -> t.getStatus() == TransferStatus.PENDING_APPROVAL &&
                             t.getFromAccount().getType() == com.bank.domain.account.AccountType.JUNIOR &&
                             t.getFromAccount().getParentAccount() != null &&
                             t.getFromAccount().getParentAccount().getCustomer().getId().equals(parent.getId()))
                .map(this::mapToResponse)
                .toList();
    }

    // -----------------------------------------------------------------------
    // Pomocnicze
    // -----------------------------------------------------------------------

    private void processInternalTransfer(Transfer transfer) {
        transfer.setStatus(TransferStatus.PROCESSING);

        try {
            Account fromAccount = transfer.getFromAccount();
            Account toAccount = transfer.getToAccount();
            Money amount = transfer.getAmount();

            if (fromAccount.getBalance().getAmount().compareTo(amount.getAmount()) < 0) {
                transfer.setStatus(TransferStatus.FAILED);
                return;
            }

            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            toAccount.setBalance(toAccount.getBalance().add(amount));
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            transactionRepository.save(Transaction.builder()
                    .account(fromAccount)
                    .amount(amount)
                    .type(TransactionType.DEBIT)
                    .status(TransactionStatus.COMPLETED)
                    .description(transfer.getDescription())
                    .referenceId(transfer.getId().toString())
                    .counterpartyName(toAccount.getCustomer().getFirstName() + " " + toAccount.getCustomer().getLastName())
                    .counterpartyIban(toAccount.getAccountNumber().getValue())
                    .build());

            transactionRepository.save(Transaction.builder()
                    .account(toAccount)
                    .amount(amount)
                    .type(TransactionType.CREDIT)
                    .status(TransactionStatus.COMPLETED)
                    .description(transfer.getDescription())
                    .referenceId(transfer.getId().toString())
                    .counterpartyName(fromAccount.getCustomer().getFirstName() + " " + fromAccount.getCustomer().getLastName())
                    .counterpartyIban(fromAccount.getAccountNumber().getValue())
                    .build());

            transfer.setStatus(TransferStatus.COMPLETED);
            transfer.setCompletedAt(LocalDateTime.now());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            transfer.setStatus(TransferStatus.FAILED);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Przelew nie powiódł się");
        }
    }

    private void validateInternalTransfer(Transfer transfer) {
        if (transfer.getChannel() != TransferChannel.INTERNAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Obsługiwane są wyłącznie przelewy wewnętrzne");
        }
        if (transfer.getFromAccount().getId().equals(transfer.getToAccount().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rachunek źródłowy i docelowy muszą być różne");
        }
        if (transfer.getFromAccount().getStatus() != AccountStatus.ACTIVE
                || transfer.getToAccount().getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Oba rachunki muszą być aktywne");
        }
        if (!transfer.getFromAccount().getBalance().getCurrency().equals(transfer.getAmount().getCurrency())
                || !transfer.getToAccount().getBalance().getCurrency().equals(transfer.getAmount().getCurrency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Waluta przelewu musi być zgodna z walutą obu rachunków");
        }
    }

    private void validateExternalTransfer(Account fromAccount, TransferRequest request) {
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rachunek źródłowy musi być aktywny");
        }

        String currency = request.currency() == null ? "" : request.currency().trim().toUpperCase();
        if (!fromAccount.getBalance().getCurrency().equals(currency)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Waluta przelewu musi być zgodna z walutą rachunku źródłowego");
        }

        if (request.channel() == TransferChannel.TARGET && (request.toBic() == null || request.toBic().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Przelew TARGET wymaga podania BIC banku odbiorcy");
        }

        if (request.channel() == TransferChannel.SEPA_INSTANT
                && request.amount().compareTo(SEPA_INSTANT_LIMIT) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Limit pojedynczej transakcji SCT Inst wynosi 100 000 EUR");
        }
    }

    private String normalizeIban(String value) {
        try {
            return IBAN.of(value).getValue();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private LocalDate validateValueDate(LocalDate requestedValueDate, TransferChannel channel) {
        LocalDate today = LocalDate.now();
        LocalDate valueDate = requestedValueDate == null ? today : requestedValueDate;

        if (valueDate.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data waluty nie może być z przeszłości");
        }
        // Dla kanałów zewnętrznych SEPA pozwala na datę przyszłą (D+1)
        if (channel == TransferChannel.INTERNAL && !valueDate.equals(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data waluty dla przelewu wewnętrznego musi być dzisiejsza");
        }
        return valueDate;
    }

    private String validateCurrency(String currency) {
        String normalized = currency == null ? "" : currency.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{3}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Waluta musi być 3-literowym kodem ISO");
        }
        return normalized;
    }

    private TransferResponse mapToResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromAccount().getId(),
                transfer.getToAccount() == null ? null : transfer.getToAccount().getId(),
                transfer.getToIban(),
                transfer.getToBic(),
                transfer.getBeneficiaryName(),
                transfer.getAmount().getAmount(),
                transfer.getAmount().getCurrency(),
                transfer.getChannel(),
                transfer.getStatus(),
                transfer.getDescription(),
                transfer.getValueDate(),
                transfer.isRequiresApproval(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt(),
                // Pola SWIFT
                transfer.getSwiftMsgId(),
                transfer.getSwiftUetr(),
                transfer.getSwiftRoute(),
                transfer.getSwiftFee(),
                transfer.getSwiftFxRate(),
                transfer.getSwiftTargetCurrency(),
                transfer.getSwiftChargeBearer()
        );
    }

    @Transactional
    public void processIncomingTransfer(
            String transferId,
            String senderBic,
            String senderIban,
            String receiverIban,
            BigDecimal amount,
            String currency,
            String description
    ) {
        log.info("Przetwarzanie przelewu przychodzącego: id={}, receiverIban={}, amount={} {}", 
                transferId, receiverIban, amount, currency);

        Account account = accountRepository.findByAccountNumber_Value(receiverIban)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Rachunek odbiorcy " + receiverIban + " nie istnieje w tym banku"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rachunek odbiorcy jest nieaktywny");
        }

        // Zwiększamy saldo konta odbiorcy
        Money moneyAmount = Money.of(amount, currency);
        account.setBalance(account.getBalance().add(moneyAmount));
        accountRepository.save(account);

        // Zapisujemy transakcję CREDIT
        transactionRepository.save(Transaction.builder()
                .account(account)
                .amount(moneyAmount)
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .referenceId(transferId)
                .counterpartyName("Przelew zewnętrzny")
                .counterpartyIban(senderIban)
                .build());

        log.info("Zaksięgowano przelew przychodzący: {} dla konta {}", moneyAmount, receiverIban);
    }

    @Transactional(readOnly = true)
    public boolean isAccountOurs(String iban) {
        if (iban == null || iban.isBlank()) {
            return false;
        }
        return accountRepository.findByAccountNumber_Value(iban).isPresent();
    }

    @Transactional
    public void completePendingSepaBatchTransfers() {
        log.info("Marking all pending SEPA Batch transfers as COMPLETED");
        List<Transfer> pending = transferRepository.findByChannelAndStatus(
                TransferChannel.SEPA, TransferStatus.PROCESSING);
        
        for (Transfer t : pending) {
            t.setStatus(TransferStatus.COMPLETED);
            t.setCompletedAt(LocalDateTime.now());
            transferRepository.save(t);
            log.info("Completed SEPA Batch transfer: {}", t.getId());
        }
    }

    @Transactional
    public void processIncomingRecallDebit(
            String transferId,
            String senderBic,
            String senderIban,
            String receiverIban,
            BigDecimal amount,
            String currency,
            String description
    ) {
        log.info("Przetwarzanie wycofania przelewu (Recall - obciążenie): id={}, receiverIban={}, amount={} {}", 
                transferId, receiverIban, amount, currency);

        Account account = accountRepository.findByAccountNumber_Value(receiverIban)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Rachunek odbiorcy " + receiverIban + " nie istnieje w tym banku"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rachunek odbiorcy jest nieaktywny");
        }

        Money moneyAmount = Money.of(amount, currency);
        account.setBalance(account.getBalance().subtract(moneyAmount));
        accountRepository.save(account);

        transactionRepository.save(Transaction.builder()
                .account(account)
                .amount(moneyAmount)
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .referenceId("RECALL-" + transferId)
                .counterpartyName("Zwrot przelewu (Recall)")
                .counterpartyIban(senderIban)
                .build());

        log.info("Zaksięgowano wycofanie przelewu (obciążenie): {} z konta {}", moneyAmount, receiverIban);
    }

    @Transactional
    public void processIncomingRecallCredit(
            String transferId,
            String senderBic,
            String senderIban,
            String receiverIban,
            BigDecimal amount,
            String currency,
            String description
    ) {
        log.info("Przetwarzanie wycofania przelewu (Recall - uznanie): id={}, senderIban={}, amount={} {}", 
                transferId, senderIban, amount, currency);

        Account account = accountRepository.findByAccountNumber_Value(senderIban)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Rachunek nadawcy " + senderIban + " nie istnieje w tym banku"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rachunek nadawcy jest nieaktywny");
        }

        Money moneyAmount = Money.of(amount, currency);
        account.setBalance(account.getBalance().add(moneyAmount));
        accountRepository.save(account);

        transactionRepository.save(Transaction.builder()
                .account(account)
                .amount(moneyAmount)
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .referenceId("RECALL-" + transferId)
                .counterpartyName("Zwrot przelewu (Recall)")
                .counterpartyIban(receiverIban)
                .build());

        log.info("Zaksięgowano wycofanie przelewu (uznanie): {} na konto {}", moneyAmount, senderIban);
    }
}
