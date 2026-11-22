package tech.afrobank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferenciaRequestDTO(
    @NotNull(message = "A conta de origem é obrigatória.")
    Long contaOrigemId,

    @NotNull(message = "A conta de destino é obrigatória.")
    Long contaDestinoId,

    @NotNull(message = "O valor da transferência é obrigatório.")
    @DecimalMin(value = "0.01", message = "O valor mínimo para transferência é de R$ 0,01.")
    BigDecimal valor
) {}
