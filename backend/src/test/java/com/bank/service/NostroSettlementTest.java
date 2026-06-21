package com.bank.service;

import com.bank.client.swift.SwiftClient;
import com.bank.domain.account.Account;
import com.bank.domain.account.AccountStatus;
import com.bank.domain.account.AccountType;
import com.bank.domain.customer.Customer;
import com.bank.domain.customer.CustomerStatus;
import com.bank.domain.shared.AccountNumber;
import com.bank.domain.shared.Money;
import com.bank.domain.swift.CorrespondentAccount;
import com.bank.domain.swift.FxRate;
import com.bank.domain.transfer.TransferChannel;
import com.bank.dto.transfer.TransferRequest;
import com.bank.repository.AccountRepository;
import com.bank.repository.CorrespondentAccountRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.FxRateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NostroSettlementTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private SwiftIncomingService swiftIncomingService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CorrespondentAccountRepository correspondentAccountRepository;

    @Autowired
    private FxRateRepository fxRateRepository;

    @MockitoBean
    private SwiftClient swiftClient;

    @Autowired
    private GermanIbanGenerator ibanGenerator;

    @Test
    public void shouldDebitNostroAccountInDifferentCurrencyWithConversion() {
        // Setup customer and standard account (in EUR)
        Customer owner = customerRepository.save(Customer.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .passportNumber("PASS123")
                .passwordHash("hash")
                .status(CustomerStatus.ACTIVE)
                .build());

        Account fromAccount = accountRepository.save(Account.builder()
                .customer(owner)
                .accountNumber(AccountNumber.of(ibanGenerator.generate()))
                .type(AccountType.STANDARD)
                .balance(Money.of(new BigDecimal("1000.00"), "EUR"))
                .reservedBalance(Money.zero("EUR"))
                .status(AccountStatus.ACTIVE)
                .build());

        // Setup active nostro account in GBP for intermediary UKBKGB01XXX
        CorrespondentAccount nostro = correspondentAccountRepository.save(CorrespondentAccount.builder()
                .correspondentBic("UKBKGB01XXX")
                .correspondentName("UK Intermediary Bank")
                .accountNumber("GB29UKBK60161331926001")
                .currency("GBP")
                .balance(new BigDecimal("500.00"))
                .status("ACTIVE")
                .build());

        // Set exchange rates:
        // EUR/USD = 1.10
        // USD/GBP = 0.80
        fxRateRepository.save(new FxRate(null, "EUR", "USD", new BigDecimal("1.10"), LocalDateTime.now()));
        fxRateRepository.save(new FxRate(null, "USD", "GBP", new BigDecimal("0.80"), LocalDateTime.now()));
        fxRateRepository.save(new FxRate(null, "GBP", "USD", new BigDecimal("1.25"), LocalDateTime.now()));

        // Mock SwiftClient response
        when(swiftClient.isEnabled()).thenReturn(true);
        when(swiftClient.sendMessage(anyString())).thenReturn(
                new SwiftClient.SwiftPaymentResponse(
                        "MSG-123",
                        "ACCEPTED",
                        List.of("BANKDEXX", "UKBKGB01XXX", "PLBKPL01XXX"),
                        10.0,
                        null,
                        null
                )
        );

        // Execute transfer in USD:
        // 100 EUR -> converted to USD (at 1.10 rate) -> 110 USD.
        // First correspondent is UKBKGB01XXX (uses GBP).
        // Our nostro GBP account should be debited.
        // Converted amount: 110 USD -> GBP (at 0.80 rate) -> 88.00 GBP.
        transferService.execute(new TransferRequest(
                fromAccount.getId(),
                "PL29888800003278123056976408",
                new BigDecimal("100.00"), // amount
                "EUR", // currency
                LocalDate.now(), // valueDate
                TransferChannel.SWIFT, // channel
                "USD transfer through UK", // description
                "PLBKPL01XXX", // toBic
                "Recipient Name", // beneficiaryName
                "DEBT", // chargeBearer
                "USD" // swiftTargetCurrency
        ), owner.getEmail());

        // Verify the nostro account has been debited in GBP
        CorrespondentAccount updatedNostro = correspondentAccountRepository.findById(nostro.getId()).orElseThrow();
        
        // Initial 500.00 GBP - 88.00 GBP = 412.00 GBP
        assertThat(updatedNostro.getBalance()).isEqualByComparingTo("412.00");
    }

    @Test
    public void shouldCreditNostroAccountOnRecallWithConversion() {
        // Setup customer and standard account (in EUR)
        Customer owner = customerRepository.save(Customer.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .passportNumber("PASS123")
                .passwordHash("hash")
                .status(CustomerStatus.ACTIVE)
                .build());

        Account fromAccount = accountRepository.save(Account.builder()
                .customer(owner)
                .accountNumber(AccountNumber.of(ibanGenerator.generate()))
                .type(AccountType.STANDARD)
                .balance(Money.of(new BigDecimal("1000.00"), "EUR"))
                .reservedBalance(Money.zero("EUR"))
                .status(AccountStatus.ACTIVE)
                .build());

        // Setup active nostro account in GBP for intermediary UKBKGB01XXX
        CorrespondentAccount nostro = correspondentAccountRepository.save(CorrespondentAccount.builder()
                .correspondentBic("UKBKGB01XXX")
                .correspondentName("UK Intermediary Bank")
                .accountNumber("GB29UKBK60161331926001")
                .currency("GBP")
                .balance(new BigDecimal("500.00"))
                .status("ACTIVE")
                .build());

        // Set exchange rates
        fxRateRepository.save(new FxRate(null, "EUR", "USD", new BigDecimal("1.10"), LocalDateTime.now()));
        fxRateRepository.save(new FxRate(null, "USD", "GBP", new BigDecimal("0.80"), LocalDateTime.now()));
        fxRateRepository.save(new FxRate(null, "USD", "EUR", new BigDecimal("0.91"), LocalDateTime.now()));

        // Mock SwiftClient response
        when(swiftClient.isEnabled()).thenReturn(true);
        when(swiftClient.sendMessage(anyString())).thenReturn(
                new SwiftClient.SwiftPaymentResponse(
                        "MSG-123",
                        "ACCEPTED",
                        List.of("BANKDEXX", "UKBKGB01XXX", "PLBKPL01XXX"),
                        10.0,
                        null,
                        null
                )
        );

        // Send SWIFT transfer (debits 88 GBP from nostro)
        var response = transferService.execute(new TransferRequest(
                fromAccount.getId(),
                "PL29888800003278123056976408",
                new BigDecimal("100.00"),
                "EUR",
                LocalDate.now(),
                TransferChannel.SWIFT,
                "USD transfer through UK",
                "PLBKPL01XXX",
                "Recipient Name",
                "DEBT",
                "USD"
        ), owner.getEmail());

        CorrespondentAccount postDebitNostro = correspondentAccountRepository.findById(nostro.getId()).orElseThrow();
        assertThat(postDebitNostro.getBalance()).isEqualByComparingTo("412.00");

        // Now process an incoming Recall return message for this UETR
        // In the parsed return message, targetAmount is 110 USD.
        // It should refund 88 GBP back to our nostro account at UKBKGB01XXX.
        String recallXml = "<Document>" +
                "<MsgId>RECALL-123</MsgId>" +
                "<UETR>" + response.swiftUetr() + "</UETR>" +
                "<IntrBkSttlmAmt Ccy=\"USD\">110.00</IntrBkSttlmAmt>" +
                "<Ustrd>Zwrot przelewu</Ustrd>" +
                "</Document>";

        swiftIncomingService.processIncoming(recallXml, true);

        // Verify the nostro account has been credited back in GBP
        CorrespondentAccount postRefundNostro = correspondentAccountRepository.findById(nostro.getId()).orElseThrow();
        assertThat(postRefundNostro.getBalance()).isEqualByComparingTo("500.00");
    }
}
