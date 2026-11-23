package tech.afrobank.application.port.in;

import tech.afrobank.domain.vo.AccountId;
import tech.afrobank.domain.vo.Money;
import tech.afrobank.domain.vo.TransactionId;

public interface TransferUseCase {

    record TransferCommand(
            AccountId sourceAccountId,
            AccountId targetAccountId,
            Money amount,
            Money fee,
            String idempotencyKey
    ) {}

    record TransferResult(
            TransactionId transactionId,
            AccountId sourceAccountId,
            AccountId targetAccountId,
            Money amount,
            Money fee
    ) {}

    TransferResult transfer(TransferCommand command);
}
