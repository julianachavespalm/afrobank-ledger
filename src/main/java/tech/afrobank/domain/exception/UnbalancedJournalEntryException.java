package tech.afrobank.domain.exception;

public class UnbalancedJournalEntryException extends DomainValidationException {

    public UnbalancedJournalEntryException(String message) {
        super(message);
    }
}
