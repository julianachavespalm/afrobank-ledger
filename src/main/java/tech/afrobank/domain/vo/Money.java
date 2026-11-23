package tech.afrobank.domain.vo;

import tech.afrobank.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    public static final Money ZERO = new Money(BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE));

    public Money {
        if (amount == null) {
            throw new DomainValidationException("Money amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainValidationException("Money amount cannot be negative");
        }
        amount = amount.setScale(SCALE, ROUNDING_MODE);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(String amount) {
        if (amount == null || amount.isBlank()) {
            throw new DomainValidationException("Money string representation cannot be null or blank");
        }
        try {
            return new Money(new BigDecimal(amount.trim()));
        } catch (NumberFormatException ex) {
            throw new DomainValidationException("Invalid numeric format for Money: " + amount);
        }
    }

    public static Money of(long value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public Money plus(Money other) {
        Objects.requireNonNull(other, "Money operand cannot be null");
        return new Money(this.amount.add(other.amount));
    }

    public Money minus(Money other) {
        Objects.requireNonNull(other, "Money operand cannot be null");
        if (this.isLessThan(other)) {
            throw new DomainValidationException("Subtraction would result in negative money amount");
        }
        return new Money(this.amount.subtract(other.amount));
    }

    public boolean isGreaterThan(Money other) {
        return this.compareTo(other) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return this.compareTo(other) >= 0;
    }

    public boolean isLessThan(Money other) {
        return this.compareTo(other) < 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        return this.compareTo(other) <= 0;
    }

    public boolean isZero() {
        return this.compareTo(ZERO) == 0;
    }

    public boolean isPositive() {
        return this.compareTo(ZERO) > 0;
    }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "Cannot compare Money with null");
        return this.amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
