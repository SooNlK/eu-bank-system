package com.bank.service;

import com.bank.repository.FxRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Serwis przewalutowania. Używa sztywnych kursów bankowych z tabeli fx_rates.
 * Waluta bazowa BANKDEXX: EUR.
 */
@Service
public class FxService {

    private static final Logger log = LoggerFactory.getLogger(FxService.class);

    private final FxRateRepository fxRateRepository;

    public FxService(FxRateRepository fxRateRepository) {
        this.fxRateRepository = fxRateRepository;
    }

    /**
     * Przelicza kwotę z fromCurrency na toCurrency.
     * Jeśli waluty są identyczne, zwraca kwotę bez zmian.
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) throw new IllegalArgumentException("Kwota nie może być null");
        String from = fromCurrency.trim().toUpperCase();
        String to   = toCurrency.trim().toUpperCase();

        if (from.equals(to)) return amount;

        BigDecimal rate = getRate(from, to);
        BigDecimal result = amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
        log.debug("FX: {} {} → {} {} (kurs: {})", amount, from, result, to, rate);
        return result;
    }

    /**
     * Zwraca kurs wymiany fromCurrency → toCurrency.
     */
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        String from = fromCurrency.trim().toUpperCase();
        String to   = toCurrency.trim().toUpperCase();

        if (from.equals(to)) return BigDecimal.ONE;

        return fxRateRepository.findByFromCurrencyAndToCurrency(from, to)
                .map(r -> r.getRate())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Brak kursu walutowego: " + from + "/" + to +
                        ". Dostępne pary: EUR/USD, EUR/GBP, EUR/PLN i odwrotności."));
    }
}
