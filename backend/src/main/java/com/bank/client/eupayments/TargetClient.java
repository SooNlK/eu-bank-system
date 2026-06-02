package com.bank.client.eupayments;

import com.bank.config.EuPaymentsProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Klient HTTP do serwisu TARGET (RTGS) – port 8001.
 * Używa RestTemplate (nie RestClient) bo nazwy kontenerów Docker
 * zawierają podkreślenia, które java.net.URI odrzuca.
 */
@Component
public class TargetClient {

    private static final Logger log = LoggerFactory.getLogger(TargetClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public TargetClient(EuPaymentsProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(new ObjectMapper());
        jacksonConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN, MediaType.ALL));
        this.restTemplate.setMessageConverters(List.of(
                new StringHttpMessageConverter(StandardCharsets.UTF_8),
                jacksonConverter
        ));
        this.baseUrl = props.targetUrl();
    }

    /**
     * Rejestruje bank w TARGET. Jeśli bank już istnieje, ignoruje błąd 409.
     */
    public void registerBank(String bic, String name) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(
                    Map.of("bic", bic, "name", name), headers);

            restTemplate.postForEntity(baseUrl + "/banks", request, String.class);
            log.info("TARGET: bank {} zarejestrowany", bic);
        } catch (org.springframework.web.client.HttpClientErrorException.Conflict e) {
            log.info("TARGET: bank {} już istnieje – pomijam rejestrację", bic);
        } catch (Exception e) {
            log.warn("TARGET: nie udało się zarejestrować banku {} – {}", bic, e.getMessage());
        }
    }

    /**
     * Sprawdza saldo banku w TARGET.
     */
    public BigDecimal getBankBalance(String bic) {
        try {
            ResponseEntity<BankInfo> response = restTemplate.getForEntity(
                    baseUrl + "/banks/" + bic, BankInfo.class);
            BankInfo info = response.getBody();
            return info != null ? info.balance() : null;
        } catch (Exception e) {
            log.warn("TARGET: nie udało się pobrać salda banku {} – {}", bic, e.getMessage());
            return null;
        }
    }

    /**
     * Zasila konto rozliczeniowe banku środkami.
     */
    public void injectLiquidity(String bic, BigDecimal amount, String currency) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                    Map.of("bank_bic", bic, "amount", amount, "currency", currency), headers);

            restTemplate.postForEntity(baseUrl + "/liquidity/injection", request, String.class);
            log.info("TARGET: zasilono płynność banku {} kwotą {} {}", bic, amount, currency);
        } catch (Exception e) {
            log.warn("TARGET: nie udało się zasilić płynności banku {} – {}", bic, e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BankInfo(
            @JsonProperty("bic") String bic,
            @JsonProperty("balance") BigDecimal balance
    ) {}
}
