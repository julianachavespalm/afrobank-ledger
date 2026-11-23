package tech.afrobank.entity;

import jakarta.persistence.*;
import lombok.*;
import tech.afrobank.exception.SaldoInsuficienteException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "contas_corrente")
public class ContaCorrente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_conta", nullable = false, unique = true, length = 20)
    private String numeroConta;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    @Column(name = "limite_cheque_especial", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal limiteChequeEspecial = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Version
    private Long version;

    @Column(name = "atualizado_em")
    @Builder.Default
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    /**
     * Regra de Domínio: Credita valor na conta
     */
    public void creditar(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor para crédito deve ser estritamente positivo.");
        }
        this.saldo = this.saldo.add(valor).setScale(2, RoundingMode.HALF_EVEN);
        this.atualizadoEm = LocalDateTime.now();
    }

    /**
     * Regra de Domínio: Debita valor respeitando o limite do cheque especial
     */
    public void debitar(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor para débito deve ser estritamente positivo.");
        }

        BigDecimal saldoDisponivel = this.saldo.add(this.limiteChequeEspecial);
        if (saldoDisponivel.compareTo(valor) < 0) {
            throw new SaldoInsuficienteException(
                String.format("Saldo insuficiente na conta %s. Disponível: %s, Solicitado: %s",
                    this.numeroConta, saldoDisponivel, valor)
            );
        }

        this.saldo = this.saldo.subtract(valor).setScale(2, RoundingMode.HALF_EVEN);
        this.atualizadoEm = LocalDateTime.now();
    }
}
