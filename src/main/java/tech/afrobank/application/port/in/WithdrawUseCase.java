package tech.afrobank.application.port.in;

import tech.afrobank.domain.vo.AccountId;
import tech.afrobank.domain.vo.Money;
import tech.afrobank.domain.vo.TransactionId;

public interface WithdrawUseCase {

    record WithdrawCommand(
            AccountId accountId,
            Money amount,
            String idempotencyKey
    ) {}

    record WithdrawResult(
            TransactionId transactionId,
            AccountId accountId,
            Money amount
    ) {}

    WithdrawResult withdraw(WithdrawCommand command);
}
