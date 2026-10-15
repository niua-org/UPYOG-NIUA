package org.upyog.dx.upyogdata.pfms.enricher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.upyog.dx.upyogdata.pfms.enums.Status;
import org.upyog.dx.upyogdata.pfms.models.PFMSCreateTransactionRequest;
import org.upyog.dx.upyogdata.pfms.models.PFMSData;
import org.upyog.dx.upyogdata.pfms.models.PFMSTransaction;
import org.upyog.dx.upyogdata.pfms.service.PFMSTransactionTransformer;
import org.upyog.dx.upyogdata.util.DxUtils;

/**
 * Enriches an incoming PFMSCreateTransactionRequest puhsed by State with internally managed fields
 * before the data is published to Kafka.
 *
 * Responsibilities:
 * 
 *   Assigns a UUID correlationId to each PFMSData batch — used as a tracking reference across Kafka, DB, and downstream systems.
 *   Assigns a unique UUID id to each PFMSTransaction.
 *   Sets initial status to Status #INITIATED.
 *   Serializes non-flat fields into a JSON string via
 *       PFMSTransactionTransformer and stores it in additionalDetails
 *       — ready for JSONB persistence by the egov persister.
 *
 * This class follows the Single Responsibility Principle — it only enriches,
 * it does not validate or persist.
 */

@Component
@RequiredArgsConstructor
public class PFMSDataEnricher {

    private final PFMSTransactionTransformer transformer;

    public void enrich(PFMSCreateTransactionRequest request) {
        for (PFMSData data : request.getData()) {
            String correlationId = DxUtils.getRandomUUID();
            data.setId(correlationId);
            for (PFMSTransaction transaction : data.getTransactions()) {
                transaction.setId(DxUtils.getRandomUUID());
                transaction.setCorrelationId(correlationId);
                transaction.setStatus(Status.INITIATED);
                transaction.setIngestionDate(null);
                transaction.setAdditionalDetails(transformer.toAdditionalDetailsJson(transaction));
            }
        }
    }
}
