package tech.afrobank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferenciaResponseDTO(
    Long transacaoId,
    Long contaOrigemId,
    Long contaDestinoId,
    BigDecimal valor,
    String status,
    LocalDateTime dataHora
) {}
