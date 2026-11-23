package tech.afrobank.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.afrobank.domain.exception.DomainValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdTypesTest {

    @Test
    @DisplayName("Should generate and parse AccountId")
    void shouldHandleAccountId() {
        AccountId id = AccountId.random();
        assertThat(id.value()).isNotNull();

        AccountId parsed = AccountId.from(id.toString());
        assertThat(parsed).isEqualTo(id);

        assertThatThrownBy(() -> new AccountId(null))
                .isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> AccountId.from("invalid-uuid"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Should generate and parse CustomerId")
    void shouldHandleCustomerId() {
        CustomerId id = CustomerId.random();
        assertThat(id.value()).isNotNull();
        assertThat(CustomerId.from(id.toString())).isEqualTo(id);

        assertThatThrownBy(() -> new CustomerId(null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Should generate and parse JournalEntryId")
    void shouldHandleJournalEntryId() {
        JournalEntryId id = JournalEntryId.random();
        assertThat(id.value()).isNotNull();
        assertThat(JournalEntryId.from(id.toString())).isEqualTo(id);

        assertThatThrownBy(() -> new JournalEntryId(null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Should generate and parse HoldId")
    void shouldHandleHoldId() {
        HoldId id = HoldId.random();
        assertThat(id.value()).isNotNull();
        assertThat(HoldId.from(id.toString())).isEqualTo(id);

        assertThatThrownBy(() -> new HoldId(null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Should generate and parse TransactionId")
    void shouldHandleTransactionId() {
        TransactionId id = TransactionId.random();
        assertThat(id.value()).isNotNull();
        assertThat(TransactionId.from(id.toString())).isEqualTo(id);

        assertThatThrownBy(() -> new TransactionId(null))
                .isInstanceOf(DomainValidationException.class);
    }
}
