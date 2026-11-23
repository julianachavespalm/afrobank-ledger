package tech.afrobank.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.afrobank.exception.SaldoInsuficienteException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes de Domínio - Entidade ContaCorrente")
class ContaCorrenteTest {

    @Test
    @DisplayName("Deve creditar valor com precisão monetária correta")
    void deveCreditarValorComSucesso() {
        ContaCorrente conta = ContaCorrente.builder()
                .numeroConta("001-A")
                .saldo(new BigDecimal("100.50"))
                .limiteChequeEspecial(new BigDecimal("500.00"))
                .build();

        conta.creditar(new BigDecimal("49.50"));

        assertThat(conta.getSaldo()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Deve debitar valor respeitando saldo disponível")
    void deveDebitarValorDentroDoSaldo() {
        ContaCorrente conta = ContaCorrente.builder()
                .numeroConta("001-A")
                .saldo(new BigDecimal("200.00"))
                .limiteChequeEspecial(new BigDecimal("100.00"))
                .build();

        conta.debitar(new BigDecimal("150.00"));

        assertThat(conta.getSaldo()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Deve permitir débito utilizando limite do cheque especial")
    void deveDebitarUtilizandoChequeEspecial() {
        ContaCorrente conta = ContaCorrente.builder()
                .numeroConta("001-A")
                .saldo(new BigDecimal("50.00"))
                .limiteChequeEspecial(new BigDecimal("100.00"))
                .build();

        conta.debitar(new BigDecimal("120.00"));

        // Saldo fica negativo em 70.00
        assertThat(conta.getSaldo()).isEqualByComparingTo(new BigDecimal("-70.00"));
    }

    @Test
    @DisplayName("Deve lançar SaldoInsuficienteException ao ultrapassar saldo + cheque especial")
    void deveLancarExcecaoAoUltrapassarLimite() {
        ContaCorrente conta = ContaCorrente.builder()
                .numeroConta("001-A")
                .saldo(new BigDecimal("50.00"))
                .limiteChequeEspecial(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> conta.debitar(new BigDecimal("100.01")))
                .isInstanceOf(SaldoInsuficienteException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    @DisplayName("Deve rejeitar valores nulos ou negativos para crédito e débito")
    void deveRejeitarValoresInvalidos() {
        ContaCorrente conta = ContaCorrente.builder()
                .numeroConta("001-A")
                .saldo(BigDecimal.ZERO)
                .limiteChequeEspecial(BigDecimal.ZERO)
                .build();

        assertThatThrownBy(() -> conta.creditar(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> conta.creditar(new BigDecimal("-10.00")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> conta.debitar(new BigDecimal("-5.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
