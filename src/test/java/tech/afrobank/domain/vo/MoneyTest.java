package tech.afrobank.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.afrobank.domain.exception.DomainValidationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("Should create Money with scale 2 and HALF_EVEN rounding")
    void shouldCreateMoneyWithScaleTwoAndHalfEvenRounding() {
        Money money = Money.of(new BigDecimal("100.555"));
        assertThat(money.amount()).isEqualTo(new BigDecimal("100.56"));

        Money moneyEven = Money.of(new BigDecimal("100.545"));
        assertThat(moneyEven.amount()).isEqualTo(new BigDecimal("100.54"));
    }

    @Test
    @DisplayName("Should create Money from string representation")
    void shouldCreateMoneyFromString() {
        Money money = Money.of("250.75");
        assertThat(money.amount()).isEqualTo(new BigDecimal("250.75"));
        assertThat(money.toString()).isEqualTo("250.75");
    }

    @Test
    @DisplayName("Should reject null or negative amount")
    void shouldRejectNullOrNegativeAmount() {
        assertThatThrownBy(() -> Money.of((BigDecimal) null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("cannot be null");

        assertThatThrownBy(() -> Money.of(new BigDecimal("-0.01")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("cannot be negative");

        assertThatThrownBy(() -> Money.of("invalid-number"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Invalid numeric format");
    }

    @Test
    @DisplayName("Should sum two Money objects correctly producing a new instance")
    void shouldSumTwoMoneyObjects() {
        Money a = Money.of("10.00");
        Money b = Money.of("5.50");
        Money result = a.plus(b);

        assertThat(result.amount()).isEqualTo(new BigDecimal("15.50"));
        assertThat(a.amount()).isEqualTo(new BigDecimal("10.00")); // Immutability
    }

    @Test
    @DisplayName("Should subtract two Money objects and reject subtraction resulting in negative value")
    void shouldSubtractTwoMoneyObjects() {
        Money a = Money.of("20.00");
        Money b = Money.of("7.25");
        Money result = a.minus(b);

        assertThat(result.amount()).isEqualTo(new BigDecimal("12.75"));

        assertThatThrownBy(() -> b.minus(a))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("negative money amount");
    }

    @Test
    @DisplayName("Should compare Money objects correctly")
    void shouldCompareMoneyObjects() {
        Money small = Money.of("10.00");
        Money equalSmall = Money.of("10.00");
        Money large = Money.of("50.00");

        assertThat(small.isLessThan(large)).isTrue();
        assertThat(large.isGreaterThan(small)).isTrue();
        assertThat(small.isGreaterThanOrEqual(equalSmall)).isTrue();
        assertThat(small.isLessThanOrEqual(equalSmall)).isTrue();
        assertThat(small.compareTo(equalSmall)).isZero();
    }

    @Test
    @DisplayName("Should verify zero and positive predicates")
    void shouldVerifyZeroAndPositivePredicates() {
        assertThat(Money.ZERO.isZero()).isTrue();
        assertThat(Money.ZERO.isPositive()).isFalse();

        Money positive = Money.of("0.01");
        assertThat(positive.isZero()).isFalse();
        assertThat(positive.isPositive()).isTrue();
    }
}
