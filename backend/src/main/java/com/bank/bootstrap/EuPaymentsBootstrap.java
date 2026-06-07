package com.bank.bootstrap;

import com.bank.client.eupayments.TargetClient;
import com.bank.config.EuPaymentsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Przy starcie aplikacji rejestruje nasz bank w symulatorze TARGET i zasila go płynnością
 * jeśli saldo wynosi 0. Działa idempotentnie – powtórny start nie powoduje błędów.
 */
@Component
public class EuPaymentsBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EuPaymentsBootstrap.class);

    private final TargetClient targetClient;
    private final EuPaymentsProperties props;

    public EuPaymentsBootstrap(TargetClient targetClient, EuPaymentsProperties props) {
        this.targetClient = targetClient;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== EU Payments Bootstrap ===");
        log.info("Rejestruję bank {} ({}) w TARGET...", props.bankBic(), props.bankName());

        targetClient.registerBank(props.bankBic(), props.bankName());

        // Rejestracja webhooka dla banku
        log.info("Bootstrap: rejestruję webhook {} dla banku {}...", props.webhookUrl(), props.bankBic());
        targetClient.registerWebhook(props.bankBic(), props.webhookUrl(), props.webhookSecret());

        BigDecimal balance = targetClient.getBankBalance(props.bankBic());
        if (balance == null) {
            log.warn("Bootstrap: nie udało się pobrać salda banku – pomijam zasilenie płynności");
            return;
        }

        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Bootstrap: saldo banku = 0, zassilam {} EUR", props.initialLiquidity());
            targetClient.injectLiquidity(props.bankBic(), props.initialLiquidity(), "EUR");
        } else {
            log.info("Bootstrap: saldo banku {} = {} EUR – pomijam zasilenie", props.bankBic(), balance);
        }

        log.info("=== EU Payments Bootstrap zakończony ===");
    }
}
