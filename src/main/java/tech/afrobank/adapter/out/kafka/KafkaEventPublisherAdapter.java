package tech.afrobank.adapter.out.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tech.afrobank.application.port.out.EventPublisherPort;

@Component
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisherAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload);
            log.info("Event successfully dispatched to topic [{}] with key [{}]", topic, key);
        } catch (Exception ex) {
            log.warn("Non-blocking failure dispatching event to Kafka topic [{}]: {}", topic, ex.getMessage());
        }
    }
}
