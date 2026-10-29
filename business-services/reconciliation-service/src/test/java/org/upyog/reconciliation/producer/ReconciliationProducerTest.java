package org.upyog.reconciliation.producer;

import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class ReconciliationProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ReconciliationProducer producer;

    @BeforeEach
    void setUp() throws Exception {
        producer = new ReconciliationProducer();
        java.lang.reflect.Field field = ReconciliationProducer.class.getDeclaredField("kafkaTemplate");
        field.setAccessible(true);
        field.set(producer, kafkaTemplate);
    }

    @Test
    @DisplayName("push sends payload to Kafka topic")
    void push_sendsToKafkaTemplate() {
        Map<String, Object> payload = Map.of("tenantId", "pg.citya");

        producer.push("reconciliation-topic", payload);

        verify(kafkaTemplate).send("reconciliation-topic", payload);
    }
}
