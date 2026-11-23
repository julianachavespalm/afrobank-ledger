package tech.afrobank.domain.model;

import tech.afrobank.domain.exception.DomainValidationException;
import tech.afrobank.domain.vo.AccountId;
import tech.afrobank.domain.vo.JournalEntryId;
import tech.afrobank.domain.vo.Money;
import tech.afrobank.domain.vo.PostingId;

import java.time.Instant;
import java.util.Objects;

public final class Posting {

    private final PostingId id;
    private final JournalEntryId journalEntryId;
    private final AccountId accountId;
    private final PostingDirection direction;
    private final Money amount;
    private final Instant recordedAt;

    public Posting(PostingId id,
                   JournalEntryId journalEntryId,
                   AccountId accountId,
                   PostingDirection direction,
                   Money amount,
                   Instant recordedAt) {
        if (id == null || journalEntryId == null || accountId == null || direction == null || amount == null) {
            throw new DomainValidationException("Posting fields cannot be null");
        }
        if (amount.isZero()) {
            throw new DomainValidationException("Posting amount must be strictly greater than zero");
        }
        this.id = id;
        this.journalEntryId = journalEntryId;
        this.accountId = accountId;
        this.direction = direction;
        this.amount = amount;
        this.recordedAt = recordedAt != null ? recordedAt : Instant.now();
    }

    public static Posting create(JournalEntryId journalEntryId,
                                 AccountId accountId,
                                 PostingDirection direction,
                                 Money amount) {
        return new Posting(PostingId.random(), journalEntryId, accountId, direction, amount, Instant.now());
    }

    public PostingId getId() {
        return id;
    }

    public JournalEntryId getJournalEntryId() {
        return journalEntryId;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public PostingDirection getDirection() {
        return direction;
    }

    public Money getAmount() {
        return amount;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Posting posting)) return false;
        return Objects.equals(id, posting.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Posting[id=%s, account=%s, %s %s]", id, accountId, direction, amount);
    }
}
