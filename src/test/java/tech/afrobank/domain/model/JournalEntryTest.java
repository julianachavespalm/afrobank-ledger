package tech.afrobank.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.afrobank.domain.exception.DomainValidationException;
import tech.afrobank.domain.exception.UnbalancedJournalEntryException;
import tech.afrobank.domain.model.JournalEntry.PostingBuilder;
import tech.afrobank.domain.vo.AccountId;
import tech.afrobank.domain.vo.Money;
import tech.afrobank.domain.vo.TransactionId;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalEntryTest {

    @Test
    @DisplayName("Should create balanced JournalEntry with postings")
    void shouldCreateBalancedJournalEntry() {
        AccountId src = AccountId.random();
        AccountId dst = AccountId.random();
        TransactionId txId = TransactionId.random();

        JournalEntry entry = JournalEntry.create(
                txId,
                EntryType.TRANSFER,
                LocalDate.now(),
                "P2P transfer",
                List.of(
                        new PostingBuilder(src, PostingDirection.DEBIT, Money.of("100.00")),
                        new PostingBuilder(dst, PostingDirection.CREDIT, Money.of("100.00"))
                )
        );

        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getTransactionId()).isEqualTo(txId);
        assertThat(entry.getEntryType()).isEqualTo(EntryType.TRANSFER);
        assertThat(entry.getPostings()).hasSize(2);
    }

    @Test
    @DisplayName("Should reject unbalanced JournalEntry")
    void shouldRejectUnbalancedJournalEntry() {
        AccountId src = AccountId.random();
        AccountId dst = AccountId.random();

        assertThatThrownBy(() -> JournalEntry.create(
                TransactionId.random(),
                EntryType.TRANSFER,
                LocalDate.now(),
                "Unbalanced",
                List.of(
                        new PostingBuilder(src, PostingDirection.DEBIT, Money.of("100.00")),
                        new PostingBuilder(dst, PostingDirection.CREDIT, Money.of("80.00"))
                )
        )).isInstanceOf(UnbalancedJournalEntryException.class);
    }

    @Test
    @DisplayName("Should create symmetric reversal JournalEntry linked to original entry")
    void shouldCreateSymmetricReversal() {
        AccountId src = AccountId.random();
        AccountId dst = AccountId.random();

        JournalEntry original = JournalEntry.create(
                TransactionId.random(),
                EntryType.TRANSFER,
                LocalDate.now(),
                "Initial transfer",
                List.of(
                        new PostingBuilder(src, PostingDirection.DEBIT, Money.of("75.00")),
                        new PostingBuilder(dst, PostingDirection.CREDIT, Money.of("75.00"))
                )
        );

        TransactionId reversalTxId = TransactionId.random();
        JournalEntry reversal = JournalEntry.createReversal(original, reversalTxId, "Customer request");

        assertThat(reversal.getEntryType()).isEqualTo(EntryType.REVERSAL);
        assertThat(reversal.getReversedEntryId()).isEqualTo(original.getId());
        assertThat(reversal.getPostings()).hasSize(2);

        // Verifica inversão simétrica das pernas
        Posting revPostingSrc = reversal.getPostings().stream()
                .filter(p -> p.getAccountId().equals(src))
                .findFirst()
                .orElseThrow();
        assertThat(revPostingSrc.getDirection()).isEqualTo(PostingDirection.CREDIT);
        assertThat(revPostingSrc.getAmount()).isEqualTo(Money.of("75.00"));

        Posting revPostingDst = reversal.getPostings().stream()
                .filter(p -> p.getAccountId().equals(dst))
                .findFirst()
                .orElseThrow();
        assertThat(revPostingDst.getDirection()).isEqualTo(PostingDirection.DEBIT);
        assertThat(revPostingDst.getAmount()).isEqualTo(Money.of("75.00"));
    }

    @Test
    @DisplayName("Should reject reversing an entry that is already a reversal")
    void shouldRejectReversingAReversal() {
        AccountId src = AccountId.random();
        AccountId dst = AccountId.random();

        JournalEntry original = JournalEntry.create(
                TransactionId.random(),
                EntryType.TRANSFER,
                LocalDate.now(),
                "Initial",
                List.of(
                        new PostingBuilder(src, PostingDirection.DEBIT, Money.of("50.00")),
                        new PostingBuilder(dst, PostingDirection.CREDIT, Money.of("50.00"))
                )
        );

        JournalEntry reversal = JournalEntry.createReversal(original, TransactionId.random(), "Reversal 1");

        assertThatThrownBy(() -> JournalEntry.createReversal(reversal, TransactionId.random(), "Reversal of reversal"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Cannot reverse an entry that is already a reversal");
    }
}
