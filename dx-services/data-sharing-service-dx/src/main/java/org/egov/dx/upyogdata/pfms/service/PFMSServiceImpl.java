package org.egov.dx.upyogdata.pfms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egov.dx.upyogdata.constants.Constants;
import org.egov.dx.upyogdata.pfms.enums.Status;
import org.egov.dx.upyogdata.pfms.models.PFMSCreateTransactionRequest;
import org.egov.dx.upyogdata.pfms.models.PFMSData;
import org.egov.dx.upyogdata.pfms.models.PFMSTransaction;
import org.egov.dx.upyogdata.pfms.producer.PFMSProducer;
import org.egov.dx.upyogdata.pfms.repository.PFMSRepository;
import org.egov.dx.upyogdata.util.DxUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PFMSServiceImpl implements PFMSService {

    private final PFMSProducer pfmsProducer;
    private final PFMSRepository pfmsRepository;

    @Override
    public Object createTransaction(PFMSCreateTransactionRequest request) {
        log.info("Received PFMS transaction request from state");

        // first validate the request that if coming voucher number of same voucher date is exisit or not
        validateDuplicates(request);
        log.info("Received PFMS transaction request from state is validated successfully");
        for (PFMSData data : request.getData()) {
            String correlationId = DxUtils.getRandomUUID();
            data.setId(correlationId);
            for (PFMSTransaction transaction : data.getTransactions()) {
                transaction.setId(DxUtils.getRandomUUID());
                transaction.setCorrelationId(correlationId);
                transaction.setStatus(Status.INITIATED);
                transaction.setIngestionDate(null);
            }
        }

        try {
            pfmsProducer.push(request).get();
            log.info("Data Received from State Pushed Successfully in Kafka Topic");
            return Constants.SUCCESS;
        } catch (Exception e) {
            log.error("Failed to publish PFMS data received from state to Kafka", e);
            throw new RuntimeException("Failed to publish data coming from state", e);
        }
    }

    /**
     * Validates that none of the incoming transactions are duplicates of
     * already-persisted records.
     *
     * A transaction is considered duplicate if the same voucherNumber
     * already exists in the DB for the same voucherDate. This combination
     * acts as a business-level unique key
     *
     *
     *Logic:
     *
     * Group all incoming transactions by their voucherDate
     * For each date group, query DB with all voucher numbers of that date
     * If any match found → throw exception with the conflicting voucher numbers
     *
     */
    private void validateDuplicates(PFMSCreateTransactionRequest request) {
        for (PFMSData data : request.getData()) {
            // Group transactions by voucherDate
            Map<java.time.LocalDate, List<String>> byDate = data.getTransactions().stream()
                    .collect(Collectors.groupingBy(
                            PFMSTransaction::getVoucherDate,
                            Collectors.mapping(PFMSTransaction::getVoucherNumber, Collectors.toList())
                    ));

            for (Map.Entry<java.time.LocalDate, List<String>> entry : byDate.entrySet()) {
                List<String> duplicates = pfmsRepository.findExistingVoucherNumbers(
                        entry.getValue(), entry.getKey());

                if (!duplicates.isEmpty()) {
                    throw new IllegalArgumentException("Duplicate vouchers found for date " + entry.getKey() + ": " + String.join(", ", duplicates));
                }
            }
        }
    }
}
