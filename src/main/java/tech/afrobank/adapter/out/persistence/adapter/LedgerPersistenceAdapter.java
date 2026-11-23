package tech.afrobank.adapter.out.persistence.adapter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tech.afrobank.adapter.out.persistence.entity.JournalEntryJpaEntity;
import tech.afrobank.adapter.out.persistence.entity.PostingJpaEntity;
import tech.afrobank.adapter.out.persistence.repository.JournalEntryJpaRepository;
import tech.afrobank.adapter.out.persistence.repository.PostingJpaRepository;
import tech.afrobank.application.port.out.LedgerRepositoryPort;
import tech.afrobank.domain.model.JournalEntry;
import tech.afrobank.domain.model.Posting;
import tech.afrobank.domain.vo.AccountId;
import tech.afrobank.domain.vo.JournalEntryId;
import tech.afrobank.domain.vo.TransactionId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class LedgerPersistenceAdapter implements LedgerRepositoryPort {

    private final JournalEntryJpaRepository journalEntryJpaRepository;
    private final PostingJpaRepository postingJpaRepository;

    public LedgerPersistenceAdapter(JournalEntryJpaRepository journalEntryJpaRepository,
                                    PostingJpaRepository postingJpaRepository) {
        this.journalEntryJpaRepository = journalEntryJpaRepository;
        this.postingJpaRepository = postingJpaRepository;
    }

    @Override
    public JournalEntry save(JournalEntry entry) {
        JournalEntryJpaEntity entity = JournalEntryJpaEntity.fromDomain(entry);
        JournalEntryJpaEntity saved = journalEntryJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<JournalEntry> findByTransactionId(TransactionId transactionId) {
        return journalEntryJpaRepository.findByTransactionId(transactionId.value())
                .map(JournalEntryJpaEntity::toDomain);
    }

    @Override
    public Optional<JournalEntry> findById(JournalEntryId id) {
        return journalEntryJpaRepository.findById(id.value())
                .map(JournalEntryJpaEntity::toDomain);
    }

    @Override
    public List<Posting> findPostingsByAccountId(AccountId accountId, int limit, int offset) {
        int pageNumber = offset / Math.max(limit, 1);
        PageRequest pageRequest = PageRequest.of(pageNumber, limit);
        return postingJpaRepository.findByAccountIdOrderByRecordedAtDesc(accountId.value(), pageRequest)
                .stream()
                .map(PostingJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Posting> findPostingsByBookingDate(LocalDate bookingDate) {
        return postingJpaRepository.findByBookingDate(bookingDate)
                .stream()
                .map(PostingJpaEntity::toDomain)
                .toList();
    }
}
