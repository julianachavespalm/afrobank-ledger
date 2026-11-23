package tech.afrobank.adapter.out.persistence.entity;

import jakarta.persistence.*;
import tech.afrobank.domain.model.EntryType;
import tech.afrobank.domain.model.JournalEntry;
import tech.afrobank.domain.vo.JournalEntryId;
import tech.afrobank.domain.vo.TransactionId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
public class JournalEntryJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 30)
    private EntryType entryType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "effective_timestamp", nullable = false)
    private Instant effectiveTimestamp;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "reversed_entry_id")
    private UUID reversedEntryId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PostingJpaEntity> postings = new ArrayList<>();

    public JournalEntryJpaEntity() {}

    public static JournalEntryJpaEntity fromDomain(JournalEntry domain) {
        JournalEntryJpaEntity entity = new JournalEntryJpaEntity();
        entity.id = domain.getId().value();
        entity.transactionId = domain.getTransactionId().value();
        entity.entryType = domain.getEntryType();
        entity.status = "POSTED";
        entity.bookingDate = domain.getBookingDate();
        entity.effectiveTimestamp = domain.getEffectiveTimestamp();
        entity.description = domain.getDescription();
        entity.reversedEntryId = domain.getReversedEntryId() != null ? domain.getReversedEntryId().value() : null;
        entity.createdAt = Instant.now();

        entity.postings = domain.getPostings().stream()
                .map(p -> PostingJpaEntity.fromDomain(p, entity))
                .toList();

        return entity;
    }

    public JournalEntry toDomain() {
        return new JournalEntry(
                new JournalEntryId(this.id),
                new TransactionId(this.transactionId),
                this.entryType,
                this.bookingDate,
                this.effectiveTimestamp,
                this.description,
                this.reversedEntryId != null ? new JournalEntryId(this.reversedEntryId) : null,
                this.postings.stream().map(PostingJpaEntity::toDomain).toList()
        );
    }

    public UUID getId() { return id; }
    public UUID getTransactionId() { return transactionId; }
    public List<PostingJpaEntity> getPostings() { return postings; }
}
