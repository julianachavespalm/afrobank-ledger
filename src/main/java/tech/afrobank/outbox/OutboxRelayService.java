package tech.afrobank.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public static final String TOPICO_TRANSACOES = "afrobank.transacoes.v1";
    public static final String TOPICO_DLQ = "afrobank.transacoes.dlq";
    private static final int LIMITE_TENTATIVAS = 3;

    public OutboxRelayService(
            OutboxEventRepository outboxEventRepository,
            @Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void despacharEventosPendentes() {
        if (kafkaTemplate == null) {
            return;
        }

        List<OutboxEvent> pendentes = outboxEventRepository.findByStatusOrderByCriadoEmAsc(
                "PENDENTE", PageRequest.of(0, 50)
        );

        for (OutboxEvent event : pendentes) {
            try {
                kafkaTemplate.send(TOPICO_TRANSACOES, event.getAggregateId(), event.getPayload());
                event.setStatus("PUBLICADO");
                event.setPublicadoEm(LocalDateTime.now());
                log.info("Evento outbox ID: {} publicado com sucesso no topico: {}", event.getId(), TOPICO_TRANSACOES);
            } catch (Exception ex) {
                event.setTentativas(event.getTentativas() + 1);
                log.warn("Falha ao despachar evento outbox ID: {}. Tentativa: {}/{}",
                        event.getId(), event.getTentativas(), LIMITE_TENTATIVAS, ex);

                if (event.getTentativas() >= LIMITE_TENTATIVAS) {
                    event.setStatus("FALHA_DLQ");
                    try {
                        kafkaTemplate.send(TOPICO_DLQ, event.getAggregateId(), event.getPayload());
                        log.error("Evento outbox ID: {} encaminhado para DLQ", event.getId());
                    } catch (Exception dlqEx) {
                        log.error("Falha ao encaminhar evento outbox ID: {} para DLQ", event.getId(), dlqEx);
                    }
                }
            }
            outboxEventRepository.save(event);
        }
    }
}
