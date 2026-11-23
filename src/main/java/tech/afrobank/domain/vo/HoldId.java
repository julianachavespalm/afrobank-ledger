package tech.afrobank.domain.vo;

import tech.afrobank.domain.exception.DomainValidationException;

import java.util.UUID;

public record HoldId(UUID value) {

    public HoldId {
        if (value == null) {
            throw new DomainValidationException("HoldId cannot be null");
        }
    }

    public static HoldId random() {
        return new HoldId(UUID.randomUUID());
    }

    public static HoldId from(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("HoldId string cannot be null or blank");
        }
        try {
            return new HoldId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("Invalid UUID format for HoldId: " + value);
        }
    }

    public static HoldId from(UUID value) {
        return new HoldId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
