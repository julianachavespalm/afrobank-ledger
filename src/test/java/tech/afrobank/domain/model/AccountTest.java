package tech.afrobank.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.afrobank.domain.exception.DomainValidationException;
import tech.afrobank.domain.exception.InsufficientBalanceException;
import tech.afrobank.domain.vo.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    @DisplayName("Should create customer account with zero balance")
    void shouldCreateCustomerAccount() {
        AccountId id = AccountId.random();
        CustomerId customerId = CustomerId.random();
        Account account = Account.createCustomerAccount(id, "2.1.1.01.0001", customerId);

        assertThat(account.getId()).isEqualTo(id);
        assertThat(account.getType()).isEqualTo(AccountType.LIABILITY);
        assertThat(account.getCurrentBalance()).isEqualTo(Money.ZERO);
        assertThat(account.getAvailableBalance()).isEqualTo(Money.ZERO);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should credit customer account and increase ledger balance")
    void shouldCreditCustomerAccount() {
        Account account = Account.createCustomerAccount(AccountId.random(), "2.1.1.01.0001", CustomerId.random());
        JournalEntryId entryId = JournalEntryId.random();

        Posting posting = Posting.create(entryId, account.getId(), PostingDirection.CREDIT, Money.of("150.00"));
        account.applyPosting(posting);

        assertThat(account.getCurrentBalance()).isEqualTo(Money.of("150.00"));
        assertThat(account.getAvailableBalance()).isEqualTo(Money.of("150.00"));
    }

    @Test
    @DisplayName("Should debit customer account when funds are sufficient")
    void shouldDebitCustomerAccountWithFunds() {
        Account account = Account.createCustomerAccount(AccountId.random(), "2.1.1.01.0001", CustomerId.random());
        JournalEntryId entryId = JournalEntryId.random();

        account.applyPosting(Posting.create(entryId, account.getId(), PostingDirection.CREDIT, Money.of("200.00")));
        account.applyPosting(Posting.create(entryId, account.getId(), PostingDirection.DEBIT, Money.of("80.00")));

        assertThat(account.getCurrentBalance()).isEqualTo(Money.of("120.00"));
        assertThat(account.getAvailableBalance()).isEqualTo(Money.of("120.00"));
    }

    @Test
    @DisplayName("Should reject debit when balance is insufficient")
    void shouldRejectDebitOnInsufficientBalance() {
        Account account = Account.createCustomerAccount(AccountId.random(), "2.1.1.01.0001", CustomerId.random());
        JournalEntryId entryId = JournalEntryId.random();

        account.applyPosting(Posting.create(entryId, account.getId(), PostingDirection.CREDIT, Money.of("50.00")));

        Posting overDebit = Posting.create(entryId, account.getId(), PostingDirection.DEBIT, Money.of("50.01"));

        assertThatThrownBy(() -> account.applyPosting(overDebit))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    @DisplayName("Should reduce available balance when hold is reserved and release it")
    void shouldHandleHoldReservation() {
        Account account = Account.createCustomerAccount(AccountId.random(), "2.1.1.01.0001", CustomerId.random());
        JournalEntryId entryId = JournalEntryId.random();
        account.applyPosting(Posting.create(entryId, account.getId(), PostingDirection.CREDIT, Money.of("100.00")));

        account.reserve(Money.of("40.00"));
        assertThat(account.getCurrentBalance()).isEqualTo(Money.of("100.00"));
        assertThat(account.getReservedBalance()).isEqualTo(Money.of("40.00"));
        assertThat(account.getAvailableBalance()).isEqualTo(Money.of("60.00"));

        account.releaseReservation(Money.of("40.00"));
        assertThat(account.getReservedBalance()).isEqualTo(Money.ZERO);
        assertThat(account.getAvailableBalance()).isEqualTo(Money.of("100.00"));
    }

    @Test
    @DisplayName("Should reject posting belonging to another account")
    void shouldRejectForeignPosting() {
        Account account = Account.createCustomerAccount(AccountId.random(), "2.1.1.01.0001", CustomerId.random());
        Posting foreignPosting = Posting.create(JournalEntryId.random(), AccountId.random(), PostingDirection.CREDIT, Money.of("10.00"));

        assertThatThrownBy(() -> account.applyPosting(foreignPosting))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("does not belong to this account");
    }

    @Test
    @DisplayName("Should increase balance on debit for Asset account")
    void shouldHandleAssetAccountDebitNature() {
        Account custody = Account.createInternalAccount(AccountId.random(), "1.1.1.01.0001", AccountType.ASSET);
        JournalEntryId entryId = JournalEntryId.random();

        custody.applyPosting(Posting.create(entryId, custody.getId(), PostingDirection.DEBIT, Money.of("500.00")));
        assertThat(custody.getCurrentBalance()).isEqualTo(Money.of("500.00"));

        custody.applyPosting(Posting.create(entryId, custody.getId(), PostingDirection.CREDIT, Money.of("200.00")));
        assertThat(custody.getCurrentBalance()).isEqualTo(Money.of("300.00"));
    }
}
