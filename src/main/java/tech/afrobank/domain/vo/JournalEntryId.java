package tech.afrobank.domain.vo;

import tech.afrobank.domain.exception.DomainValidationException;

import java.util.UUID;

public record JournalEntryId(UUID value) {

    public JournalEntryId {
        if (value == null) {
            throw new DomainValidationException("JournalEntryId cannot be null");
        }
    }

    public static JournalEntryId random() {
        return new JournalEntryId(UUID.randomUUID());
    }

    public static JournalEntryId from(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("JournalEntryId string cannot be null or blank");
        }
        try {
            return new JournalEntryId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("Invalid UUID format for JournalEntryId: " + value);
        }
    }

    public static JournalEntryId from(UUID value) {
        return new JournalEntryId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
