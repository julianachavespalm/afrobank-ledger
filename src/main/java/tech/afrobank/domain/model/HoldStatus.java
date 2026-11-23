package tech.afrobank.domain.model;

public enum HoldStatus {
    PENDING,
    ACTIVE,
    CAPTURED,
    VOIDED,
    EXPIRED,
    REJECTED;

    public boolean isTerminal() {
        return this == CAPTURED || this == VOIDED || this == EXPIRED || this == REJECTED;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }
}
