package tech.afrobank.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferenciaRealizadaEvent(
    Long transacaoId,
    Long contaOrigemId,
    Long contaDestinoId,
    BigDecimal valor,
    String status,
    LocalDateTime dataHora
) {}
