package org.upyog.family.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Event producer publishing family lifecycle events to Kafka topics for asynchronous consumption in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes an event payload to a specified Kafka topic for asynchronous processing in UPYOG.
     * Logs the target topic name and delegates to Spring KafkaTemplate.
     *
     * @param topic destination Kafka topic name
     * @param value serializable event payload object
     */
    public void push(String topic, Object value) {
        log.info("[UPYOG-KAFKA-PRODUCER] Publishing event to topic: {}", topic);
        try {
            kafkaTemplate.send(topic, value);
        } catch (Exception e) {
            log.warn("[UPYOG-KAFKA-PRODUCER] Failed to publish event to topic {}: {}", topic, e.getMessage());
        }
    }
}
