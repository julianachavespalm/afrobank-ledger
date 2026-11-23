package tech.afrobank.application.port.in;

import tech.afrobank.domain.vo.Money;

import java.time.LocalDate;

public interface TrialBalanceQuery {

    record TrialBalanceResult(
            LocalDate bookingDate,
            Money totalDebits,
            Money totalCredits,
            Money netDifference,
            boolean isBalanced
    ) {}

    TrialBalanceResult calculateTrialBalance(LocalDate bookingDate);
}
