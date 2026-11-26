package com.ingestpipeline.consumer;

import com.ingestpipeline.producer.IngestProducer;
import com.ingestpipeline.service.ValidationService;
import com.ingestpipeline.util.ApplicationProperties;
import com.ingestpipeline.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatorConsumerTest {

    @Mock
    private ValidationService validationService;
    @Mock
    private IngestProducer ingestProducer;
    @Mock
    private ApplicationProperties applicationProperties;

    @InjectMocks
    private ValidatorConsumer validatorConsumer;

    @Test
    void processMessage_invalidPayload_routesToErrorTopic() {
        Map<String, Object> payload = Map.of("dataContext", "x");
        when(validationService.validateData(payload)).thenReturn(false);

        validatorConsumer.processMessage(payload, Constants.KafkaTopics.INGEST_DATA);

        verify(ingestProducer).pushToPipeline(eq(payload), eq(Constants.KafkaTopics.ERROR_INTENT), eq(null));
    }

    @Test
    void processMessage_validPayload_routesToTransformTopicWhenEnabled() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("dataContext", "collection");
        when(validationService.validateData(payload)).thenReturn(true);

        Map<String, Boolean> rules = new HashMap<>();
        rules.put(Constants.PipelineRules.TRANSFORM_DATA, true);
        when(applicationProperties.getPipelineRules()).thenReturn(rules);
        when(applicationProperties.getTransactionValidationTopic()).thenReturn("validate-topic");
        when(applicationProperties.getTransactionValidationKey()).thenReturn("validate-key");

        validatorConsumer.processMessage(payload, Constants.KafkaTopics.INGEST_DATA);

        verify(ingestProducer).pushToPipeline(eq(payload), eq("validate-topic"), eq(null));
    }
}
