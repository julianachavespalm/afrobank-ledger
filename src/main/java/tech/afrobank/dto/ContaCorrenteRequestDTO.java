package tech.afrobank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ContaCorrenteRequestDTO(
    @NotBlank(message = "O número da conta é obrigatório.")
    String numeroConta,

    @NotNull(message = "O ID do cliente é obrigatório.")
    Long clienteId,

    @PositiveOrZero(message = "O saldo inicial não pode ser negativo.")
    BigDecimal saldoInicial,

    @PositiveOrZero(message = "O limite de cheque especial não pode ser negativo.")
    BigDecimal limiteChequeEspecial
) {}
