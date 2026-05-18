package com.bank.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class IBAN {

    @Column(name = "iban", unique = true, nullable = false, length = 34)
    private String value;

    public static IBAN of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("IBAN cannot be empty");
        }
        String normalized = value.replaceAll("\\s+", "").toUpperCase();
        if (!isValid(normalized)) {
            throw new IllegalArgumentException("Invalid IBAN");
        }
        return new IBAN(normalized);
    }

    private static boolean isValid(String iban) {
        if (!iban.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}")) {
            return false;
        }

        String rearranged = iban.substring(4) + iban.substring(0, 4);
        int remainder = 0;
        for (int i = 0; i < rearranged.length(); i++) {
            char current = rearranged.charAt(i);
            if (Character.isDigit(current)) {
                remainder = (remainder * 10 + Character.digit(current, 10)) % 97;
            } else if (current >= 'A' && current <= 'Z') {
                int converted = current - 'A' + 10;
                remainder = (remainder * 10 + converted / 10) % 97;
                remainder = (remainder * 10 + converted % 10) % 97;
            } else {
                return false;
            }
        }
        return remainder == 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IBAN iban = (IBAN) o;
        return value.equals(iban.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
