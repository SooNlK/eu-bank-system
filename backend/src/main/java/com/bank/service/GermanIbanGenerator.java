package com.bank.service;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.SecureRandom;

@Component
public class GermanIbanGenerator {

    private static final String COUNTRY_CODE = "DE";
    private static final String BANK_CODE = "10011001";
    private static final int ACCOUNT_NUMBER_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        String accountNumber = randomAccountNumber();
        String bban = BANK_CODE + accountNumber;
        String checkDigits = calculateCheckDigits(bban);
        return COUNTRY_CODE + checkDigits + bban;
    }

    private String randomAccountNumber() {
        long value = RANDOM.nextLong(10_000_000_000L);
        return String.format("%0" + ACCOUNT_NUMBER_LENGTH + "d", value);
    }

    private String calculateCheckDigits(String bban) {
        String rearranged = bban + COUNTRY_CODE + "00";
        StringBuilder numeric = new StringBuilder();

        for (char character : rearranged.toCharArray()) {
            if (Character.isDigit(character)) {
                numeric.append(character);
            } else if (Character.isLetter(character)) {
                numeric.append(Character.toUpperCase(character) - 'A' + 10);
            } else {
                throw new IllegalArgumentException("Invalid IBAN character: " + character);
            }
        }

        int checksum = 98 - new BigInteger(numeric.toString()).mod(BigInteger.valueOf(97)).intValue();
        return String.format("%02d", checksum);
    }
}
