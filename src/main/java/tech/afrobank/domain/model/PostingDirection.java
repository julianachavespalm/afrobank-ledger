package tech.afrobank.domain.model;

public enum PostingDirection {
    DEBIT,
    CREDIT;

    public PostingDirection opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
