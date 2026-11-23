package tech.afrobank.application.port.in;

import tech.afrobank.domain.vo.AccountId;
import tech.afrobank.domain.vo.Money;
import tech.afrobank.domain.vo.TransactionId;

public interface DepositUseCase {

    record DepositCommand(
            AccountId accountId,
            Money amount,
            String idempotencyKey
    ) {}

    record DepositResult(
            TransactionId transactionId,
            AccountId accountId,
            Money amount
    ) {}

    DepositResult deposit(DepositCommand command);
}
