package tech.afrobank.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Unidade - OutboxRelayService")
class OutboxRelayServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxRelayService outboxRelayService;

    @Test
    @DisplayName("Deve despachar eventos pendentes para o topico Kafka e atualizar status para PUBLICADO")
    void deveDespacharEventosComSucesso() {
        OutboxEvent event = OutboxEvent.builder()
                .id(1L)
                .aggregateId("001")
                .aggregateType("CONTA_CORRENTE")
                .eventType("TRANSFERENCIA_REALIZADA")
                .payload("{\"valor\":100.00}")
                .status("PENDENTE")
                .build();

        when(outboxEventRepository.findByStatusOrderByCriadoEmAsc(eq("PENDENTE"), any(Pageable.class)))
                .thenReturn(List.of(event));

        outboxRelayService.despacharEventosPendentes();

        verify(kafkaTemplate).send(eq(OutboxRelayService.TOPICO_TRANSACOES), eq("001"), eq("{\"valor\":100.00}"));
        verify(outboxEventRepository).save(argThat(e -> "PUBLICADO".equals(e.getStatus())));
    }

    @Test
    @DisplayName("Deve incrementar tentativas e mover para DLQ apos limite de falhas")
    void deveMoverParaDLQAposLimiteFalhas() {
        OutboxEvent event = OutboxEvent.builder()
                .id(2L)
                .aggregateId("002")
                .aggregateType("CONTA_CORRENTE")
                .eventType("TRANSFERENCIA_REALIZADA")
                .payload("{\"valor\":50.00}")
                .status("PENDENTE")
                .tentativas(2)
                .build();

        when(outboxEventRepository.findByStatusOrderByCriadoEmAsc(eq("PENDENTE"), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(eq(OutboxRelayService.TOPICO_TRANSACOES), anyString(), anyString()))
                .thenThrow(new RuntimeException("Broker Kafka indisponivel"));

        outboxRelayService.despacharEventosPendentes();

        verify(kafkaTemplate).send(eq(OutboxRelayService.TOPICO_DLQ), eq("002"), eq("{\"valor\":50.00}"));
        verify(outboxEventRepository).save(argThat(e -> "FALHA_DLQ".equals(e.getStatus()) && e.getTentativas() == 3));
    }
}
