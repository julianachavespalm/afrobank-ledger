package tech.afrobank.domain.model;

public enum AccountStatus {
    ACTIVE,
    BLOCKED,
    CLOSED;

    public boolean canDebit() {
        return this == ACTIVE;
    }

    public boolean canCredit() {
        return this == ACTIVE || this == BLOCKED;
    }
}
