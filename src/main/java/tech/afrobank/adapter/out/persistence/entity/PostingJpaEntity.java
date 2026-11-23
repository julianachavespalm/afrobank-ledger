package tech.afrobank.adapter.out.persistence.entity;

import jakarta.persistence.*;
import tech.afrobank.domain.model.Posting;
import tech.afrobank.domain.model.PostingDirection;
import tech.afrobank.domain.vo.AccountId;
import tech.afrobank.domain.vo.JournalEntryId;
import tech.afrobank.domain.vo.Money;
import tech.afrobank.domain.vo.PostingId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "postings")
public class PostingJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntryJpaEntity journalEntry;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private PostingDirection direction;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    public PostingJpaEntity() {}

    public static PostingJpaEntity fromDomain(Posting domain, JournalEntryJpaEntity journalEntry) {
        PostingJpaEntity entity = new PostingJpaEntity();
        entity.id = domain.getId().value();
        entity.journalEntry = journalEntry;
        entity.accountId = domain.getAccountId().value();
        entity.direction = domain.getDirection();
        entity.amount = domain.getAmount().amount();
        entity.recordedAt = domain.getRecordedAt();
        return entity;
    }

    public Posting toDomain() {
        return new Posting(
                new PostingId(this.id),
                new JournalEntryId(this.journalEntry.getId()),
                new AccountId(this.accountId),
                this.direction,
                Money.of(this.amount),
                this.recordedAt
        );
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public PostingDirection getDirection() { return direction; }
    public BigDecimal getAmount() { return amount; }
    public Instant getRecordedAt() { return recordedAt; }
}
