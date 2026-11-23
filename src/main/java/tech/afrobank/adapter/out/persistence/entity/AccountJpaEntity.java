package tech.afrobank.adapter.out.persistence.entity;

import jakarta.persistence.*;
import tech.afrobank.domain.model.Account;
import tech.afrobank.domain.model.AccountStatus;
import tech.afrobank.domain.model.AccountType;
import tech.afrobank.domain.vo.AccountId;
import tech.afrobank.domain.vo.CustomerId;
import tech.afrobank.domain.vo.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "account_code", nullable = false, unique = true, length = 30)
    private String accountCode;

    @Column(name = "customer_id")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "reserved_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal reservedBalance;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AccountJpaEntity() {}

    public static AccountJpaEntity fromDomain(Account domain) {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.id = domain.getId().value();
        entity.accountCode = domain.getAccountCode();
        entity.customerId = domain.getCustomerId() != null ? domain.getCustomerId().value() : null;
        entity.accountType = domain.getType();
        entity.status = domain.getStatus();
        entity.currentBalance = domain.getCurrentBalance().amount();
        entity.reservedBalance = domain.getReservedBalance().amount();
        entity.version = domain.getVersion();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        return entity;
    }

    public Account toDomain() {
        return new Account(
                new AccountId(this.id),
                this.accountCode,
                this.customerId != null ? new CustomerId(this.customerId) : null,
                this.accountType,
                this.status,
                Money.of(this.currentBalance),
                Money.of(this.reservedBalance),
                this.version,
                this.createdAt,
                this.updatedAt
        );
    }

    public UUID getId() { return id; }
    public String getAccountCode() { return accountCode; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public BigDecimal getReservedBalance() { return reservedBalance; }
}
