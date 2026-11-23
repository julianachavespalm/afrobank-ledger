package tech.afrobank.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.afrobank.application.port.in.DepositUseCase;
import tech.afrobank.application.port.in.TransferUseCase;
import tech.afrobank.application.port.in.TrialBalanceQuery;
import tech.afrobank.application.port.in.WithdrawUseCase;
import tech.afrobank.application.port.out.AccountRepositoryPort;
import tech.afrobank.application.port.out.EventPublisherPort;
import tech.afrobank.application.port.out.LedgerRepositoryPort;
import tech.afrobank.domain.exception.DomainValidationException;
import tech.afrobank.domain.model.Account;
import tech.afrobank.domain.model.EntryType;
import tech.afrobank.domain.model.JournalEntry;
import tech.afrobank.domain.model.JournalEntry.PostingBuilder;
import tech.afrobank.domain.model.Posting;
import tech.afrobank.domain.model.PostingDirection;
import tech.afrobank.domain.vo.AccountId;
import tech.afrobank.domain.vo.Money;
import tech.afrobank.domain.vo.TransactionId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class LedgerService implements DepositUseCase, WithdrawUseCase, TransferUseCase, TrialBalanceQuery {

    public static final String CUSTODY_ACCOUNT_CODE = "1.1.1.01.0001";
    public static final String FEE_REVENUE_ACCOUNT_CODE = "4.1.1.01.0001";

    private final AccountRepositoryPort accountRepository;
    private final LedgerRepositoryPort ledgerRepository;
    private final EventPublisherPort eventPublisher;

    public LedgerService(AccountRepositoryPort accountRepository,
                         LedgerRepositoryPort ledgerRepository,
                         EventPublisherPort eventPublisher) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public DepositResult deposit(DepositCommand command) {
        Objects.requireNonNull(command, "DepositCommand cannot be null");
        if (command.amount().isZero()) {
            throw new DomainValidationException("Deposit amount must be strictly positive");
        }

        Account custodyAccount = getAccountByCode(CUSTODY_ACCOUNT_CODE);
        Account customerAccount = getAccountByIdForUpdate(command.accountId());

        TransactionId txId = TransactionId.random();

        JournalEntry entry = JournalEntry.create(
                txId,
                EntryType.DEPOSIT,
                LocalDate.now(),
                "External deposit into account " + customerAccount.getAccountCode(),
                List.of(
                        new PostingBuilder(custodyAccount.getId(), PostingDirection.DEBIT, command.amount()),
                        new PostingBuilder(customerAccount.getId(), PostingDirection.CREDIT, command.amount())
                )
        );

        // Aplica o assento contábil e atualiza saldos
        applyEntryToAccounts(entry, List.of(custodyAccount, customerAccount));

        ledgerRepository.save(entry);
        accountRepository.save(custodyAccount);
        accountRepository.save(customerAccount);

        eventPublisher.publish("afrobank.ledger.entries.v1", txId.toString(), entry);

        return new DepositResult(txId, customerAccount.getId(), command.amount());
    }

    @Override
    public WithdrawResult withdraw(WithdrawCommand command) {
        Objects.requireNonNull(command, "WithdrawCommand cannot be null");
        if (command.amount().isZero()) {
            throw new DomainValidationException("Withdraw amount must be strictly positive");
        }

        Account customerAccount = getAccountByIdForUpdate(command.accountId());
        Account custodyAccount = getAccountByCode(CUSTODY_ACCOUNT_CODE);

        TransactionId txId = TransactionId.random();

        JournalEntry entry = JournalEntry.create(
                txId,
                EntryType.WITHDRAWAL,
                LocalDate.now(),
                "External cash withdrawal from account " + customerAccount.getAccountCode(),
                List.of(
                        new PostingBuilder(customerAccount.getId(), PostingDirection.DEBIT, command.amount()),
                        new PostingBuilder(custodyAccount.getId(), PostingDirection.CREDIT, command.amount())
                )
        );

        applyEntryToAccounts(entry, List.of(customerAccount, custodyAccount));

        ledgerRepository.save(entry);
        accountRepository.save(customerAccount);
        accountRepository.save(custodyAccount);

        eventPublisher.publish("afrobank.ledger.entries.v1", txId.toString(), entry);

        return new WithdrawResult(txId, customerAccount.getId(), command.amount());
    }

    @Override
    public TransferResult transfer(TransferCommand command) {
        Objects.requireNonNull(command, "TransferCommand cannot be null");
        if (command.sourceAccountId().equals(command.targetAccountId())) {
            throw new DomainValidationException("Source and target accounts must be distinct");
        }
        if (command.amount().isZero()) {
            throw new DomainValidationException("Transfer amount must be strictly positive");
        }

        Money fee = command.fee() != null ? command.fee() : Money.ZERO;

        // Prevenção de deadlocks: busca de contas sob ordenação determinística de identificadores
        List<AccountId> accountIdsToLock = new ArrayList<>();
        accountIdsToLock.add(command.sourceAccountId());
        accountIdsToLock.add(command.targetAccountId());

        Account feeAccount = null;
        if (fee.isPositive()) {
            feeAccount = getAccountByCode(FEE_REVENUE_ACCOUNT_CODE);
            accountIdsToLock.add(feeAccount.getId());
        }

        List<Account> lockedAccounts = accountRepository.findByIdsForUpdate(accountIdsToLock);

        Account sourceAccount = findInList(lockedAccounts, command.sourceAccountId());
        Account targetAccount = findInList(lockedAccounts, command.targetAccountId());

        TransactionId txId = TransactionId.random();
        List<PostingBuilder> builders = new ArrayList<>();

        if (fee.isPositive()) {
            Money totalDebit = command.amount().plus(fee);
            builders.add(new PostingBuilder(sourceAccount.getId(), PostingDirection.DEBIT, totalDebit));
            builders.add(new PostingBuilder(targetAccount.getId(), PostingDirection.CREDIT, command.amount()));
            builders.add(new PostingBuilder(feeAccount.getId(), PostingDirection.CREDIT, fee));
        } else {
            builders.add(new PostingBuilder(sourceAccount.getId(), PostingDirection.DEBIT, command.amount()));
            builders.add(new PostingBuilder(targetAccount.getId(), PostingDirection.CREDIT, command.amount()));
        }

        JournalEntry entry = JournalEntry.create(
                txId,
                fee.isPositive() ? EntryType.TRANSFER_WITH_FEE : EntryType.TRANSFER,
                LocalDate.now(),
                String.format("Transfer from %s to %s", sourceAccount.getAccountCode(), targetAccount.getAccountCode()),
                builders
        );

        applyEntryToAccounts(entry, lockedAccounts);

        ledgerRepository.save(entry);
        for (Account acc : lockedAccounts) {
            accountRepository.save(acc);
        }

        eventPublisher.publish("afrobank.ledger.entries.v1", txId.toString(), entry);

        return new TransferResult(txId, sourceAccount.getId(), targetAccount.getId(), command.amount(), fee);
    }

    @Override
    @Transactional(readOnly = true)
    public TrialBalanceResult calculateTrialBalance(LocalDate bookingDate) {
        LocalDate date = bookingDate != null ? bookingDate : LocalDate.now();
        List<Posting> postings = ledgerRepository.findPostingsByBookingDate(date);

        Money totalDebits = Money.ZERO;
        Money totalCredits = Money.ZERO;

        for (Posting posting : postings) {
            if (posting.getDirection() == PostingDirection.DEBIT) {
                totalDebits = totalDebits.plus(posting.getAmount());
            } else {
                totalCredits = totalCredits.plus(posting.getAmount());
            }
        }

        Money difference = totalDebits.isGreaterThanOrEqual(totalCredits)
                ? totalDebits.minus(totalCredits)
                : totalCredits.minus(totalDebits);

        return new TrialBalanceResult(date, totalDebits, totalCredits, difference, difference.isZero());
    }

    private void applyEntryToAccounts(JournalEntry entry, List<Account> accounts) {
        for (Posting posting : entry.getPostings()) {
            Account account = findInList(accounts, posting.getAccountId());
            account.applyPosting(posting);
        }
    }

    private Account getAccountByCode(String code) {
        return accountRepository.findByAccountCode(code)
                .orElseThrow(() -> new DomainValidationException("System account not found: " + code));
    }

    private Account getAccountByIdForUpdate(AccountId id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new DomainValidationException("Account not found: " + id));
    }

    private Account findInList(List<Account> accounts, AccountId id) {
        return accounts.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new DomainValidationException("Account not found in locked set: " + id));
    }
}
