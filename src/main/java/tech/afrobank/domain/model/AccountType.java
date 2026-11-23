package tech.afrobank.domain.model;

public enum AccountType {
    ASSET(true),
    LIABILITY(false),
    EQUITY(false),
    REVENUE(false),
    EXPENSE(true);

    private final boolean debitNature;

    AccountType(boolean debitNature) {
        this.debitNature = debitNature;
    }

    public boolean isDebitNature() {
        return debitNature;
    }

    public boolean isCreditNature() {
        return !debitNature;
    }
}
