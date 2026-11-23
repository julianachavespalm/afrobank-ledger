package tech.afrobank.domain.exception;

public class InsufficientBalanceException extends DomainValidationException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
