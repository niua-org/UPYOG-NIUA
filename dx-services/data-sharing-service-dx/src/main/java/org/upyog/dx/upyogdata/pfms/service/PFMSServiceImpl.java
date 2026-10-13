package org.upyog.dx.upyogdata.pfms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.upyog.dx.upyogdata.constants.Constants;
import org.upyog.dx.upyogdata.pfms.enricher.PFMSDataEnricher;
import org.upyog.dx.upyogdata.pfms.models.*;
import org.upyog.dx.upyogdata.pfms.producer.PFMSProducer;
import org.upyog.dx.upyogdata.pfms.repository.PFMSRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class PFMSServiceImpl implements PFMSService {

    private final PFMSProducer pfmsProducer;
    private final PFMSRepository pfmsRepository;
    private final PFMSDataEnricher enricher;


    @Override
    public Object createTransaction(PFMSCreateTransactionRequest request) {
        log.info("Received PFMS transaction request from state");

        // first validate the request that if coming voucher number of same voucher date is exisit or not
        validateDuplicates(request);
        log.info("Received PFMS transaction request from state is validated successfully");

        enricher.enrich(request); // enrich the request

        /**
         * Builds a flat list of PFMSTransactionResponse.TransactionResult from the enriched request.
         *
         *  Each PFMSData in the request can contain multiple PFMSTransaction objects.
         *  flatMap flattens this two-level nested structure (List of PFMSData → List of PFMSTransaction)
         * into a single stream, and each transaction is mapped to a lightweight TransactionResult
         * carrying only the assigned id, voucherNumber, and a SUCCESS status.
         * This list is returned to the caller as part of the API response
         */
        List<PFMSTransactionResponse.TransactionResult> results = request.getData().stream()
                .flatMap(data -> data.getTransactions().stream())
                .map(t -> PFMSTransactionResponse.TransactionResult.builder()
                        .id(t.getId())
                        .voucherNumber(t.getVoucherNumber())
                        .status(Constants.SUCCESS)
                        .build())
                .collect(Collectors.toList());

        try {
            pfmsProducer.push(request).get(); // using .get() to confirm that kafka successfully publihsed the event
            log.info("Data Received from State Pushed Successfully in Kafka Topic");
        } catch (Exception e) {
            log.error("Failed to publish PFMS data received from state to Kafka", e);
            throw new RuntimeException("Failed to publish data coming from state", e);
        }

        /**
         * Builds the API response following the Upyog standard response structure.
         *
         * The final PFMSTransactionResponse bundles the responseInfo, id and the per-transaction results list into a single
         * structured response object
         */
        RequestInfo requestInfo = request.getRequestInfo();
        ResponseInfo responseInfo = ResponseInfo.builder()
                .apiId(requestInfo != null ? requestInfo.getApiId() : null)
                .ver(requestInfo != null ? requestInfo.getVer() : null)
                .ts(Instant.now().toEpochMilli())
                .msgId(requestInfo != null ? requestInfo.getMsgId() : null)
                .resMsgId(Constants.SUCCESS_MESSAGE)
                .status(Constants.SUCCESS)
                .build();

        String correlationId = request.getData().isEmpty() ? null : request.getData().get(0).getId();
        return PFMSTransactionResponse.builder()
                .responseInfo(responseInfo)
                .correlationId(correlationId)
                .transactions(results)
                .build();
    }

    /**
     * Validates that none of the incoming transactions are duplicates of
     * already-persisted records.
     *
     * A transaction is considered duplicate if the same voucherNumber
     * already exists in the DB for the same pfms agency code. This combination
     * acts as a business-level unique key
     *
     *
     *Logic:
     *
     * Group all incoming transactions by their voucherDate
     * For each date group, query DB with all voucher numbers of teh same pfms agency code
     * If any match found → throw exception with the conflicting voucher numbers
     *
     */
    private void validateDuplicates(PFMSCreateTransactionRequest request) {
        for (PFMSData data : request.getData()) {

            // Check for duplicates within the incoming request itself
            Set<String> voucherLists = new HashSet<>();
            for (PFMSTransaction t : data.getTransactions()) {
                String key = t.getUlbCodeOrPFMSAgencyCode() + "|" + t.getVoucherNumber();
                if (!voucherLists.add(key)) {
                    throw new IllegalArgumentException(
                            "Duplicate voucher in request - ulbCode: " + t.getUlbCodeOrPFMSAgencyCode()
                                    + ", voucherNumber: " + t.getVoucherNumber());
                }
            }

            // Group transactions by pfms agencycode
            Map<String, List<String>> byUlb = data.getTransactions().stream()
                    .collect(Collectors.groupingBy(
                            PFMSTransaction::getUlbCodeOrPFMSAgencyCode,
                            Collectors.mapping(PFMSTransaction::getVoucherNumber, Collectors.toList())
                    ));

            for (Map.Entry<String, List<String>> entry : byUlb.entrySet()) {
                List<String> duplicates = pfmsRepository.findExistingVoucherNumbers(entry.getValue(), entry.getKey());

                if (!duplicates.isEmpty()) {
                    throw new IllegalArgumentException("Duplicate vouchers for ulbCode " + entry.getKey() + ": " + String.join(", ", duplicates));
                }
            }
        }
    }
}
