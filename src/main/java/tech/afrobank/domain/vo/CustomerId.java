package tech.afrobank.domain.vo;

import tech.afrobank.domain.exception.DomainValidationException;

import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new DomainValidationException("CustomerId cannot be null");
        }
    }

    public static CustomerId random() {
        return new CustomerId(UUID.randomUUID());
    }

    public static CustomerId from(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("CustomerId string cannot be null or blank");
        }
        try {
            return new CustomerId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("Invalid UUID format for CustomerId: " + value);
        }
    }

    public static CustomerId from(UUID value) {
        return new CustomerId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
