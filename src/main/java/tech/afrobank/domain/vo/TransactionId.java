package tech.afrobank.domain.vo;

import tech.afrobank.domain.exception.DomainValidationException;

import java.util.UUID;

public record TransactionId(UUID value) {

    public TransactionId {
        if (value == null) {
            throw new DomainValidationException("TransactionId cannot be null");
        }
    }

    public static TransactionId random() {
        return new TransactionId(UUID.randomUUID());
    }

    public static TransactionId from(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("TransactionId string cannot be null or blank");
        }
        try {
            return new TransactionId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("Invalid UUID format for TransactionId: " + value);
        }
    }

    public static TransactionId from(UUID value) {
        return new TransactionId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
