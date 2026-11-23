package tech.afrobank.application.port.in;

import tech.afrobank.domain.vo.AccountId;
import tech.afrobank.domain.vo.HoldId;
import tech.afrobank.domain.vo.Money;

import java.time.Duration;

public interface CreateHoldUseCase {

    record CreateHoldCommand(
            AccountId accountId,
            Money amount,
            Duration ttl,
            String idempotencyKey
    ) {}

    record HoldResult(
            HoldId holdId,
            AccountId accountId,
            Money amount
    ) {}

    HoldResult createHold(CreateHoldCommand command);
}
