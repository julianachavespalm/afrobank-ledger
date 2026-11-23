package tech.afrobank.domain.validator;

import tech.afrobank.domain.exception.DomainValidationException;
import tech.afrobank.domain.exception.UnbalancedJournalEntryException;
import tech.afrobank.domain.model.PostingDirection;
import tech.afrobank.domain.vo.Money;

import java.util.List;

public class DoubleEntryBalanceValidator {

    public record EntryLeg(PostingDirection direction, Money amount) {}

    public static void validateZeroBalance(List<EntryLeg> legs) {
        if (legs == null || legs.size() < 2) {
            throw new DomainValidationException("A journal entry must contain at least two postings");
        }

        Money totalDebits = Money.ZERO;
        Money totalCredits = Money.ZERO;

        for (EntryLeg leg : legs) {
            if (leg == null || leg.amount() == null || leg.direction() == null) {
                throw new DomainValidationException("Posting leg, direction and amount cannot be null");
            }
            if (leg.amount().isZero()) {
                throw new DomainValidationException("Posting amount must be strictly greater than zero");
            }

            if (leg.direction() == PostingDirection.DEBIT) {
                totalDebits = totalDebits.plus(leg.amount());
            } else {
                totalCredits = totalCredits.plus(leg.amount());
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new UnbalancedJournalEntryException(
                    String.format("Unbalanced journal entry: Total debits (%s) must equal total credits (%s)",
                            totalDebits, totalCredits)
            );
        }
    }
}
