package org.upyog.draft.producer;

import org.egov.tracer.kafka.CustomKafkaTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProducerTest {

    @Mock
    private CustomKafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private Producer producer;

    @Test
    void push_validTopicAndPayload_sendsToKafka() {
        Object payload = java.util.Map.of("draftId", "d-1");

        producer.push("persister-save-draft", payload);

        verify(kafkaTemplate).send("persister-save-draft", payload);
    }
}
