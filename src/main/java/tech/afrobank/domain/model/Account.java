package tech.afrobank.domain.model;

import tech.afrobank.domain.exception.DomainValidationException;
import tech.afrobank.domain.exception.InsufficientBalanceException;
import tech.afrobank.domain.vo.AccountId;
import tech.afrobank.domain.vo.CustomerId;
import tech.afrobank.domain.vo.Money;

import java.time.Instant;
import java.util.Objects;

public final class Account {

    private final AccountId id;
    private final String accountCode;
    private final CustomerId customerId;
    private final AccountType type;
    private AccountStatus status;
    private Money currentBalance;
    private Money reservedBalance;
    private Long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public Account(AccountId id,
                   String accountCode,
                   CustomerId customerId,
                   AccountType type,
                   AccountStatus status,
                   Money currentBalance,
                   Money reservedBalance,
                   Long version,
                   Instant createdAt,
                   Instant updatedAt) {
        if (id == null || accountCode == null || type == null || status == null) {
            throw new DomainValidationException("Account required attributes cannot be null");
        }
        this.id = id;
        this.accountCode = accountCode;
        this.customerId = customerId;
        this.type = type;
        this.status = status;
        this.currentBalance = currentBalance != null ? currentBalance : Money.ZERO;
        this.reservedBalance = reservedBalance != null ? reservedBalance : Money.ZERO;
        this.version = version;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static Account createCustomerAccount(AccountId id,
                                                String accountCode,
                                                CustomerId customerId) {
        return new Account(
                id,
                accountCode,
                customerId,
                AccountType.LIABILITY,
                AccountStatus.ACTIVE,
                Money.ZERO,
                Money.ZERO,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    public static Account createInternalAccount(AccountId id,
                                                String accountCode,
                                                AccountType type) {
        return new Account(
                id,
                accountCode,
                null,
                type,
                AccountStatus.ACTIVE,
                Money.ZERO,
                Money.ZERO,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    public Money getAvailableBalance() {
        if (type == AccountType.LIABILITY) {
            if (currentBalance.isGreaterThanOrEqual(reservedBalance)) {
                return currentBalance.minus(reservedBalance);
            }
            return Money.ZERO;
        }
        return currentBalance;
    }

    public boolean canDebit(Money amount) {
        Objects.requireNonNull(amount, "Amount to debit cannot be null");
        if (!status.canDebit()) {
            return false;
        }
        if (type == AccountType.LIABILITY) {
            return getAvailableBalance().isGreaterThanOrEqual(amount);
        }
        return true;
    }

    public void applyPosting(Posting posting) {
        Objects.requireNonNull(posting, "Posting cannot be null");
        if (!Objects.equals(this.id, posting.getAccountId())) {
            throw new DomainValidationException("Posting does not belong to this account");
        }

        PostingDirection direction = posting.getDirection();
        Money amount = posting.getAmount();

        if (type.isDebitNature()) {
            // Contas de natureza devedora (ex: Ativo / Custódia Externa)
            if (direction == PostingDirection.DEBIT) {
                this.currentBalance = this.currentBalance.plus(amount);
            } else {
                this.currentBalance = this.currentBalance.minus(amount);
            }
        } else {
            // Contas de natureza credora (ex: Passivo / Conta Corrente de Clientes)
            if (direction == PostingDirection.CREDIT) {
                if (!status.canCredit()) {
                    throw new DomainValidationException("Account status does not permit credit: " + status);
                }
                this.currentBalance = this.currentBalance.plus(amount);
            } else {
                if (!canDebit(amount)) {
                    throw new InsufficientBalanceException(
                            String.format("Insufficient funds in account %s. Available: %s, Requested debit: %s",
                                    accountCode, getAvailableBalance(), amount)
                    );
                }
                this.currentBalance = this.currentBalance.minus(amount);
            }
        }
        this.updatedAt = Instant.now();
    }

    public void reserve(Money amount) {
        Objects.requireNonNull(amount, "Amount to reserve cannot be null");
        if (!canDebit(amount)) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient available balance for hold reservation in account %s", accountCode)
            );
        }
        this.reservedBalance = this.reservedBalance.plus(amount);
        this.updatedAt = Instant.now();
    }

    public void releaseReservation(Money amount) {
        Objects.requireNonNull(amount, "Amount to release cannot be null");
        this.reservedBalance = this.reservedBalance.minus(amount);
        this.updatedAt = Instant.now();
    }

    public void setStatus(AccountStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.updatedAt = Instant.now();
    }

    public AccountId getId() {
        return id;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public AccountType getType() {
        return type;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Money getCurrentBalance() {
        return currentBalance;
    }

    public Money getReservedBalance() {
        return reservedBalance;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
