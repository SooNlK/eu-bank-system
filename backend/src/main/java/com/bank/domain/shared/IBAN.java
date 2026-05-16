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
        // A simple validation for IBAN length can be added here
        String normalized = value.replaceAll("\\s+", "");
        return new IBAN(normalized);
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
