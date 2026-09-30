package org.egov.dx.upyogdata.pfms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egov.dx.upyogdata.pfms.client.PFMSApiClient;
import org.egov.dx.upyogdata.pfms.enums.SchedulerType;
import org.egov.dx.upyogdata.pfms.models.PFMSTransaction;
import org.egov.dx.upyogdata.pfms.models.SchedulerLog;
import org.egov.dx.upyogdata.pfms.repository.PFMSRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.egov.dx.upyogdata.constants.Constants;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.egov.dx.upyogdata.constants.Constants;

@Slf4j
@Service
@RequiredArgsConstructor
public class PFMSForwardingServiceImpl implements PFMSForwardingService {

    private final PFMSRepository pfmsRepository;
    private final PFMSApiClient pfmsApiClient;

    @Value("${pfms.scheduler.batch.size:50}")
    private int batchSize;

    private String currentToken;

    @Override
    public void forwardInitiatedTransactions() {
        processBatch(pfmsRepository.fetchInitiatedTransactionIds(batchSize), SchedulerType.INITIATED_FORWARD);
    }

    @Override
    public void retryFailedTransactions() {
        processBatch(pfmsRepository.fetchFailedTransactionIds(batchSize), SchedulerType.FAILED_RETRY);
    }

    private void processBatch(List<String> ids, SchedulerType type) {
        log.info("Pfms Scheduler [{}] Starting cycle", type);

        if (ids.isEmpty()) {
            log.info("Pfms Scheduler [{}] No transactions found. Skipping.", type);
            return;
        }

        log.info("Pfms Scheduler [{}] Found {} transaction(s)", type, ids.size());
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            currentToken = pfmsApiClient.fetchAccessToken();
        } catch (Exception e) {
            log.error("Pfms Scheduler [{}] Auth failed, aborting cycle", type, e);
            return;
        }

        int success = 0, failed = 0;
        for (String id : ids) {
            try {
                PFMSTransaction transaction = pfmsRepository.fetchTransactionById(id);
                pfmsRepository.updateTransactionStatus(id, Constants.PROCESSING, null);
                log.info("Pfms [{}] Processing | id={} voucherNumber={}", type, id, transaction.getVoucherNumber());

                String pfmsResponse = pushWithTokenRefreshOnExpiry(transaction);

                pfmsRepository.updateTransactionStatus(id, pfmsResponse.trim(), LocalDateTime.now());
                log.info("Pfms [{}] Success | id={} response={}", type, id, pfmsResponse);
                success++;

            } catch (Exception e) {
                log.error("Pfms[{}] Failed | id={}", type, id, e);
                pfmsRepository.updateTransactionStatus(id, Constants.FAILED, LocalDateTime.now());
                failed++;
            }
        }

        LocalDateTime endedAt = LocalDateTime.now();
        String cycleStatus = (failed == 0) ? Constants.COMPLETED : (success == 0) ? Constants.ALL_FAILED : Constants.PARTIAL;

        log.info("Pfms Scheduler [{}] Cycle complete | total={} success={} failed={} status={}",
                type, ids.size(), success, failed, cycleStatus);

        pfmsRepository.saveSchedulerLog(SchedulerLog.builder()
                .schedulerType(type.name())
                .startedAt(startedAt)
                .endedAt(endedAt)
                .durationMs(ChronoUnit.MILLIS.between(startedAt, endedAt))
                .status(cycleStatus)
                .totalPicked(ids.size())
                .successCount(success)
                .failedCount(failed)
                .createdBy("SCHEDULER")
                .build());
    }

    private String pushWithTokenRefreshOnExpiry(PFMSTransaction transaction) {
        try {
            return pfmsApiClient.pushTransaction(transaction, currentToken);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Pfms Token expired (401), refreshing | voucherNumber={}", transaction.getVoucherNumber());
            currentToken = pfmsApiClient.fetchAccessToken();
            return pfmsApiClient.pushTransaction(transaction, currentToken);
        }
    }
}
