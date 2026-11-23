package tech.afrobank.application.port.in;

import tech.afrobank.domain.vo.HoldId;

public interface VoidHoldUseCase {

    record VoidHoldCommand(
            HoldId holdId,
            String reason,
            String idempotencyKey
    ) {}

    void voidHold(VoidHoldCommand command);
}
