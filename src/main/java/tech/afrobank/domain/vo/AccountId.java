package tech.afrobank.domain.vo;

import tech.afrobank.domain.exception.DomainValidationException;

import java.util.UUID;

public record AccountId(UUID value) {

    public AccountId {
        if (value == null) {
            throw new DomainValidationException("AccountId cannot be null");
        }
    }

    public static AccountId random() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId from(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("AccountId string cannot be null or blank");
        }
        try {
            return new AccountId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("Invalid UUID format for AccountId: " + value);
        }
    }

    public static AccountId from(UUID value) {
        return new AccountId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
