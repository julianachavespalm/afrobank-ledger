package tech.afrobank.domain.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.afrobank.domain.exception.DomainValidationException;
import tech.afrobank.domain.exception.UnbalancedJournalEntryException;
import tech.afrobank.domain.model.PostingDirection;
import tech.afrobank.domain.validator.DoubleEntryBalanceValidator.EntryLeg;
import tech.afrobank.domain.vo.Money;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DoubleEntryBalanceValidatorTest {

    @Test
    @DisplayName("Should accept balanced journal entry with 2 postings")
    void shouldAcceptBalancedTwoLegEntry() {
        List<EntryLeg> legs = List.of(
                new EntryLeg(PostingDirection.DEBIT, Money.of("100.00")),
                new EntryLeg(PostingDirection.CREDIT, Money.of("100.00"))
        );

        assertThatCode(() -> DoubleEntryBalanceValidator.validateZeroBalance(legs))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept balanced multi-leg journal entry (transfer with fee split)")
    void shouldAcceptBalancedMultiLegEntry() {
        List<EntryLeg> legs = List.of(
                new EntryLeg(PostingDirection.DEBIT, Money.of("101.50")),
                new EntryLeg(PostingDirection.CREDIT, Money.of("100.00")),
                new EntryLeg(PostingDirection.CREDIT, Money.of("1.50"))
        );

        assertThatCode(() -> DoubleEntryBalanceValidator.validateZeroBalance(legs))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject unbalanced journal entry")
    void shouldRejectUnbalancedEntry() {
        List<EntryLeg> legs = List.of(
                new EntryLeg(PostingDirection.DEBIT, Money.of("100.00")),
                new EntryLeg(PostingDirection.CREDIT, Money.of("90.00"))
        );

        assertThatThrownBy(() -> DoubleEntryBalanceValidator.validateZeroBalance(legs))
                .isInstanceOf(UnbalancedJournalEntryException.class)
                .hasMessageContaining("Total debits (100.00) must equal total credits (90.00)");
    }

    @Test
    @DisplayName("Should reject entries with fewer than two postings or zero amount")
    void shouldRejectInvalidLegs() {
        assertThatThrownBy(() -> DoubleEntryBalanceValidator.validateZeroBalance(List.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("at least two postings");

        assertThatThrownBy(() -> DoubleEntryBalanceValidator.validateZeroBalance(List.of(
                new EntryLeg(PostingDirection.DEBIT, Money.of("100.00"))
        ))).isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> DoubleEntryBalanceValidator.validateZeroBalance(List.of(
                new EntryLeg(PostingDirection.DEBIT, Money.ZERO),
                new EntryLeg(PostingDirection.CREDIT, Money.ZERO)
        ))).isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("greater than zero");
    }
}
