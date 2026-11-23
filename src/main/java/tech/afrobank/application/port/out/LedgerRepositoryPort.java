package tech.afrobank.application.port.out;

import tech.afrobank.domain.model.JournalEntry;
import tech.afrobank.domain.model.Posting;
import tech.afrobank.domain.vo.AccountId;
import tech.afrobank.domain.vo.JournalEntryId;
import tech.afrobank.domain.vo.TransactionId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LedgerRepositoryPort {

    JournalEntry save(JournalEntry entry);

    Optional<JournalEntry> findByTransactionId(TransactionId transactionId);

    Optional<JournalEntry> findById(JournalEntryId id);

    List<Posting> findPostingsByAccountId(AccountId accountId, int limit, int offset);

    List<Posting> findPostingsByBookingDate(LocalDate bookingDate);
}
