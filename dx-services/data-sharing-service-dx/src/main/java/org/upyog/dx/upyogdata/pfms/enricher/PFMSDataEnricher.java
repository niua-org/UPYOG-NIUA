package org.upyog.dx.upyogdata.pfms.enricher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.upyog.dx.upyogdata.pfms.models.PFMSCreateTransactionKafkaMessage;
import org.upyog.dx.upyogdata.pfms.models.PFMSCreateTransactionRequest;
import org.upyog.dx.upyogdata.pfms.service.PFMSTransactionTransformer;

/**
 * Transforms inbound API contract payloads into Kafka persistence entities.
 */
@Component
@RequiredArgsConstructor
public class PFMSDataEnricher {

    private final PFMSTransactionTransformer transformer;

    public PFMSCreateTransactionKafkaMessage enrich(PFMSCreateTransactionRequest request) {
        return transformer.toKafkaMessage(request);
    }
}
