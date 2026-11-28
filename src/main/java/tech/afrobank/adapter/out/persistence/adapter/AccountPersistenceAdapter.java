package tech.afrobank.adapter.out.persistence.adapter;

import org.springframework.stereotype.Component;
import tech.afrobank.adapter.out.persistence.entity.AccountJpaEntity;
import tech.afrobank.adapter.out.persistence.repository.AccountJpaRepository;
import tech.afrobank.application.port.out.AccountRepositoryPort;
import tech.afrobank.domain.model.Account;
import tech.afrobank.domain.vo.AccountId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AccountPersistenceAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository accountJpaRepository;

    public AccountPersistenceAdapter(AccountJpaRepository accountJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return accountJpaRepository.findById(id.value()).map(AccountJpaEntity::toDomain);
    }

    @Override
    public Optional<Account> findByAccountCode(String accountCode) {
        return accountJpaRepository.findByAccountCode(accountCode).map(AccountJpaEntity::toDomain);
    }

    @Override
    public Optional<Account> findByIdForUpdate(AccountId id) {
        return accountJpaRepository.findByIdForUpdate(id.value()).map(AccountJpaEntity::toDomain);
    }

    @Override
    public List<Account> findByIdsForUpdate(List<AccountId> ids) {
        List<UUID> rawIds = ids.stream().map(AccountId::value).toList();
        return accountJpaRepository.findByIdsForUpdate(rawIds).stream()
                .map(AccountJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = AccountJpaEntity.fromDomain(account);
        AccountJpaEntity saved = accountJpaRepository.save(entity);
        return saved.toDomain();
    }
}
