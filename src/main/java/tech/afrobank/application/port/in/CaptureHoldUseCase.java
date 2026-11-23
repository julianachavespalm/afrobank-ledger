package tech.afrobank.application.port.in;

import tech.afrobank.domain.vo.AccountId;
import tech.afrobank.domain.vo.HoldId;
import tech.afrobank.domain.vo.TransactionId;

public interface CaptureHoldUseCase {

    record CaptureHoldCommand(
            HoldId holdId,
            AccountId targetAccountId,
            String idempotencyKey
    ) {}

    record CaptureResult(
            HoldId holdId,
            TransactionId transactionId
    ) {}

    CaptureResult capture(CaptureHoldCommand command);
}
