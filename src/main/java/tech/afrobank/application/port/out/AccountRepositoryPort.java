package tech.afrobank.application.port.out;

import tech.afrobank.domain.model.Account;
import tech.afrobank.domain.vo.AccountId;

import java.util.List;
import java.util.Optional;

public interface AccountRepositoryPort {

    Optional<Account> findById(AccountId id);

    Optional<Account> findByAccountCode(String accountCode);

    Optional<Account> findByIdForUpdate(AccountId id);

    List<Account> findByIdsForUpdate(List<AccountId> ids);

    Account save(Account account);
}
