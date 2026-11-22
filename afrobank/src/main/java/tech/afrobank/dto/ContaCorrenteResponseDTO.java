package tech.afrobank.dto;

import tech.afrobank.entity.ContaCorrente;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContaCorrenteResponseDTO(
    Long id,
    String numeroConta,
    BigDecimal saldo,
    BigDecimal limiteChequeEspecial,
    BigDecimal saldoTotalDisponivel,
    Long clienteId,
    String clienteNome,
    LocalDateTime atualizadoEm
) {
    public static ContaCorrenteResponseDTO fromEntity(ContaCorrente conta) {
        BigDecimal totalDisponivel = conta.getSaldo().add(conta.getLimiteChequeEspecial());
        return new ContaCorrenteResponseDTO(
            conta.getId(),
            conta.getNumeroConta(),
            conta.getSaldo(),
            conta.getLimiteChequeEspecial(),
            totalDisponivel,
            conta.getCliente() != null ? conta.getCliente().getId() : null,
            conta.getCliente() != null ? conta.getCliente().getNome() : null,
            conta.getAtualizadoEm()
        );
    }
}
