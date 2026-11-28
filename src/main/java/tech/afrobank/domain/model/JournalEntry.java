package tech.afrobank.domain.model;

import tech.afrobank.domain.exception.DomainValidationException;
import tech.afrobank.domain.validator.DoubleEntryBalanceValidator;
import tech.afrobank.domain.validator.DoubleEntryBalanceValidator.EntryLeg;
import tech.afrobank.domain.vo.JournalEntryId;
import tech.afrobank.domain.vo.TransactionId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class JournalEntry {

    private final JournalEntryId id;
    private final TransactionId transactionId;
    private final EntryType entryType;
    private final LocalDate bookingDate;
    private final Instant effectiveTimestamp;
    private final String description;
    private final JournalEntryId reversedEntryId;
    private final List<Posting> postings;

    public JournalEntry(JournalEntryId id,
                        TransactionId transactionId,
                        EntryType entryType,
                        LocalDate bookingDate,
                        Instant effectiveTimestamp,
                        String description,
                        JournalEntryId reversedEntryId,
                        List<Posting> postings) {
        if (id == null || transactionId == null || entryType == null || bookingDate == null || postings == null) {
            throw new DomainValidationException("JournalEntry required fields cannot be null");
        }

        // Valida invariante fundamental de partidas dobradas
        List<EntryLeg> legs = postings.stream()
                .map(p -> new EntryLeg(p.getDirection(), p.getAmount()))
                .toList();
        DoubleEntryBalanceValidator.validateZeroBalance(legs);

        this.id = id;
        this.transactionId = transactionId;
        this.entryType = entryType;
        this.bookingDate = bookingDate;
        this.effectiveTimestamp = effectiveTimestamp != null ? effectiveTimestamp : Instant.now();
        this.description = description != null ? description : "";
        this.reversedEntryId = reversedEntryId;
        this.postings = Collections.unmodifiableList(postings);
    }

    public static JournalEntry create(TransactionId transactionId,
                                      EntryType entryType,
                                      LocalDate bookingDate,
                                      String description,
                                      List<PostingBuilder> legBuilders) {
        JournalEntryId entryId = JournalEntryId.random();
        Instant now = Instant.now();

        List<Posting> postings = legBuilders.stream()
                .map(builder -> builder.build(entryId, now))
                .toList();

        return new JournalEntry(entryId, transactionId, entryType, bookingDate, now, description, null, postings);
    }

    public static JournalEntry createReversal(JournalEntry originalEntry,
                                             TransactionId reversalTxId,
                                             String reason) {
        Objects.requireNonNull(originalEntry, "Original entry to reverse cannot be null");
        if (originalEntry.getEntryType() == EntryType.REVERSAL) {
            throw new DomainValidationException("Cannot reverse an entry that is already a reversal");
        }

        JournalEntryId reversalId = JournalEntryId.random();
        Instant now = Instant.now();

        // Inversão simétrica das pernas contábeis
        List<Posting> reversalPostings = originalEntry.getPostings().stream()
                .map(p -> new Posting(
                        tech.afrobank.domain.vo.PostingId.random(),
                        reversalId,
                        p.getAccountId(),
                        p.getDirection().opposite(),
                        p.getAmount(),
                        now
                ))
                .toList();

        return new JournalEntry(
                reversalId,
                reversalTxId,
                EntryType.REVERSAL,
                LocalDate.now(),
                now,
                "Reversal of " + originalEntry.getId().value() + ": " + reason,
                originalEntry.getId(),
                reversalPostings
        );
    }

    public JournalEntryId getId() {
        return id;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public Instant getEffectiveTimestamp() {
        return effectiveTimestamp;
    }

    public String getDescription() {
        return description;
    }

    public JournalEntryId getReversedEntryId() {
        return reversedEntryId;
    }

    public List<Posting> getPostings() {
        return postings;
    }

    public record PostingBuilder(
            tech.afrobank.domain.vo.AccountId accountId,
            PostingDirection direction,
            tech.afrobank.domain.vo.Money amount
    ) {
        Posting build(JournalEntryId entryId, Instant timestamp) {
            return new Posting(
                    tech.afrobank.domain.vo.PostingId.random(),
                    entryId,
                    accountId,
                    direction,
                    amount,
                    timestamp
            );
        }
    }
}
