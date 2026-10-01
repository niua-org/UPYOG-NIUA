package org.egov.dx.upyogdata.pfms.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egov.dx.upyogdata.config.KafkaConfig;
import org.egov.dx.upyogdata.pfms.models.PFMSCreateTransactionRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class PFMSProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaConfig kafkaConfig;

    public CompletableFuture<SendResult<String, Object>> push(
            PFMSCreateTransactionRequest request) {
        log.info("Publishing PFMS transaction request to Kafka topic: {}", kafkaConfig.getTopic());
        return kafkaTemplate.send(kafkaConfig.getTopic(), request);
    }

}