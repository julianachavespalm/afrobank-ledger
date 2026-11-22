package tech.afrobank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OperacaoFinanceiraRequestDTO(
    @NotNull(message = "O valor da operação é obrigatório.")
    @DecimalMin(value = "0.01", message = "O valor mínimo para a operação é de R$ 0,01.")
    BigDecimal valor
) {}
