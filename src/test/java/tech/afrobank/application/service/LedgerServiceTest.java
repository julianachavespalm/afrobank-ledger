package tech.afrobank.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.afrobank.application.port.in.DepositUseCase.DepositCommand;
import tech.afrobank.application.port.in.DepositUseCase.DepositResult;
import tech.afrobank.application.port.in.TransferUseCase.TransferCommand;
import tech.afrobank.application.port.in.TransferUseCase.TransferResult;
import tech.afrobank.application.port.in.TrialBalanceQuery.TrialBalanceResult;
import tech.afrobank.application.port.in.WithdrawUseCase.WithdrawCommand;
import tech.afrobank.application.port.in.WithdrawUseCase.WithdrawResult;
import tech.afrobank.application.port.out.AccountRepositoryPort;
import tech.afrobank.application.port.out.EventPublisherPort;
import tech.afrobank.application.port.out.LedgerRepositoryPort;
import tech.afrobank.domain.exception.DomainValidationException;
import tech.afrobank.domain.exception.InsufficientBalanceException;
import tech.afrobank.domain.model.*;
import tech.afrobank.domain.vo.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private AccountRepositoryPort accountRepository;

    @Mock
    private LedgerRepositoryPort ledgerRepository;

    @Mock
    private EventPublisherPort eventPublisher;

    private LedgerService ledgerService;

    private Account custodyAccount;
    private Account feeAccount;
    private Account customerA;
    private Account customerB;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(accountRepository, ledgerRepository, eventPublisher);

        custodyAccount = Account.createInternalAccount(AccountId.random(), LedgerService.CUSTODY_ACCOUNT_CODE, AccountType.ASSET);
        feeAccount = Account.createInternalAccount(AccountId.random(), LedgerService.FEE_REVENUE_ACCOUNT_CODE, AccountType.REVENUE);

        customerA = Account.createCustomerAccount(AccountId.random(), "2.1.1.01.0001", CustomerId.random());
        customerB = Account.createCustomerAccount(AccountId.random(), "2.1.1.01.0002", CustomerId.random());
    }

    @Test
    @DisplayName("Should execute deposit in double-entry (Debit Custody, Credit Customer)")
    void shouldExecuteDeposit() {
        when(accountRepository.findByAccountCode(LedgerService.CUSTODY_ACCOUNT_CODE)).thenReturn(Optional.of(custodyAccount));
        when(accountRepository.findByIdForUpdate(customerA.getId())).thenReturn(Optional.of(customerA));

        DepositResult result = ledgerService.deposit(new DepositCommand(customerA.getId(), Money.of("300.00"), "idem-1"));

        assertThat(result.amount()).isEqualTo(Money.of("300.00"));
        assertThat(customerA.getCurrentBalance()).isEqualTo(Money.of("300.00"));
        assertThat(custodyAccount.getCurrentBalance()).isEqualTo(Money.of("300.00"));

        verify(ledgerRepository).save(any(JournalEntry.class));
        verify(accountRepository).save(custodyAccount);
        verify(accountRepository).save(customerA);
        verify(eventPublisher).publish(eq("afrobank.ledger.entries.v1"), any(), any());
    }

    @Test
    @DisplayName("Should execute withdrawal when balance is sufficient")
    void shouldExecuteWithdrawal() {
        // Pré-carrega saldo
        customerA.applyPosting(Posting.create(JournalEntryId.random(), customerA.getId(), PostingDirection.CREDIT, Money.of("200.00")));
        custodyAccount.applyPosting(Posting.create(JournalEntryId.random(), custodyAccount.getId(), PostingDirection.DEBIT, Money.of("200.00")));

        when(accountRepository.findByIdForUpdate(customerA.getId())).thenReturn(Optional.of(customerA));
        when(accountRepository.findByAccountCode(LedgerService.CUSTODY_ACCOUNT_CODE)).thenReturn(Optional.of(custodyAccount));

        WithdrawResult result = ledgerService.withdraw(new WithdrawCommand(customerA.getId(), Money.of("80.00"), "idem-2"));

        assertThat(result.amount()).isEqualTo(Money.of("80.00"));
        assertThat(customerA.getCurrentBalance()).isEqualTo(Money.of("120.00"));
        assertThat(custodyAccount.getCurrentBalance()).isEqualTo(Money.of("120.00"));

        verify(ledgerRepository).save(any(JournalEntry.class));
    }

    @Test
    @DisplayName("Should reject withdrawal on insufficient balance")
    void shouldRejectWithdrawalOnInsufficientBalance() {
        when(accountRepository.findByIdForUpdate(customerA.getId())).thenReturn(Optional.of(customerA));
        when(accountRepository.findByAccountCode(LedgerService.CUSTODY_ACCOUNT_CODE)).thenReturn(Optional.of(custodyAccount));

        assertThatThrownBy(() -> ledgerService.withdraw(new WithdrawCommand(customerA.getId(), Money.of("10.00"), "idem-3")))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(ledgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should execute P2P transfer between customer accounts")
    void shouldExecuteTransfer() {
        customerA.applyPosting(Posting.create(JournalEntryId.random(), customerA.getId(), PostingDirection.CREDIT, Money.of("500.00")));

        when(accountRepository.findByIdsForUpdate(anyList())).thenReturn(List.of(customerA, customerB));

        TransferResult result = ledgerService.transfer(new TransferCommand(
                customerA.getId(),
                customerB.getId(),
                Money.of("150.00"),
                Money.ZERO,
                "idem-4"
        ));

        assertThat(result.amount()).isEqualTo(Money.of("150.00"));
        assertThat(customerA.getCurrentBalance()).isEqualTo(Money.of("350.00"));
        assertThat(customerB.getCurrentBalance()).isEqualTo(Money.of("150.00"));

        verify(ledgerRepository).save(any(JournalEntry.class));
    }

    @Test
    @DisplayName("Should execute transfer with fee split (Debit A, Credit B, Credit Fee Account)")
    void shouldExecuteTransferWithFeeSplit() {
        customerA.applyPosting(Posting.create(JournalEntryId.random(), customerA.getId(), PostingDirection.CREDIT, Money.of("200.00")));

        when(accountRepository.findByAccountCode(LedgerService.FEE_REVENUE_ACCOUNT_CODE)).thenReturn(Optional.of(feeAccount));
        when(accountRepository.findByIdsForUpdate(anyList())).thenReturn(List.of(customerA, customerB, feeAccount));

        TransferResult result = ledgerService.transfer(new TransferCommand(
                customerA.getId(),
                customerB.getId(),
                Money.of("100.00"),
                Money.of("2.50"),
                "idem-5"
        ));

        assertThat(result.amount()).isEqualTo(Money.of("100.00"));
        assertThat(result.fee()).isEqualTo(Money.of("2.50"));

        // A debitado em 102.50, B creditado em 100.00, Receita creditada em 2.50
        assertThat(customerA.getCurrentBalance()).isEqualTo(Money.of("97.50"));
        assertThat(customerB.getCurrentBalance()).isEqualTo(Money.of("100.00"));
        assertThat(feeAccount.getCurrentBalance()).isEqualTo(Money.of("2.50"));

        verify(ledgerRepository).save(any(JournalEntry.class));
    }

    @Test
    @DisplayName("Should reject transfer between same accounts")
    void shouldRejectTransferToSameAccount() {
        assertThatThrownBy(() -> ledgerService.transfer(new TransferCommand(
                customerA.getId(),
                customerA.getId(),
                Money.of("10.00"),
                Money.ZERO,
                "idem-6"
        ))).isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("must be distinct");
    }

    @Test
    @DisplayName("Should calculate trial balance confirming zero difference")
    void shouldCalculateTrialBalance() {
        JournalEntryId entryId = JournalEntryId.random();
        List<Posting> postings = List.of(
                Posting.create(entryId, customerA.getId(), PostingDirection.DEBIT, Money.of("100.00")),
                Posting.create(entryId, customerB.getId(), PostingDirection.CREDIT, Money.of("100.00")),
                Posting.create(entryId, custodyAccount.getId(), PostingDirection.DEBIT, Money.of("50.00")),
                Posting.create(entryId, customerA.getId(), PostingDirection.CREDIT, Money.of("50.00"))
        );

        when(ledgerRepository.findPostingsByBookingDate(LocalDate.now())).thenReturn(postings);

        TrialBalanceResult result = ledgerService.calculateTrialBalance(LocalDate.now());

        assertThat(result.isBalanced()).isTrue();
        assertThat(result.totalDebits()).isEqualTo(Money.of("150.00"));
        assertThat(result.totalCredits()).isEqualTo(Money.of("150.00"));
        assertThat(result.netDifference()).isEqualTo(Money.ZERO);
    }
}
