package tech.afrobank.domain.vo;

import tech.afrobank.domain.exception.DomainValidationException;

import java.util.UUID;

public record PostingId(UUID value) {

    public PostingId {
        if (value == null) {
            throw new DomainValidationException("PostingId cannot be null");
        }
    }

    public static PostingId random() {
        return new PostingId(UUID.randomUUID());
    }

    public static PostingId from(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("PostingId string cannot be null or blank");
        }
        try {
            return new PostingId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("Invalid UUID format for PostingId: " + value);
        }
    }

    public static PostingId from(UUID value) {
        return new PostingId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
