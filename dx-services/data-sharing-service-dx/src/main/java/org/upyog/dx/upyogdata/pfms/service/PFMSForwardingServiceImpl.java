package org.upyog.dx.upyogdata.pfms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.upyog.dx.upyogdata.config.PFMSProperties;
import org.upyog.dx.upyogdata.constants.Constants;
import org.upyog.dx.upyogdata.pfms.client.PFMSApiClient;
import org.upyog.dx.upyogdata.pfms.enums.SchedulerType;
import org.upyog.dx.upyogdata.pfms.entity.PfmsTransactionEntity;
import org.upyog.dx.upyogdata.pfms.models.SchedulerLog;
import org.upyog.dx.upyogdata.pfms.repository.PFMSRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import feign.FeignException;
import org.upyog.dx.upyogdata.util.DxUtils;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Forwards stored PFMS transactions to the external PFMS APIs.
 * <p>
 * Auth and data-push HTTP is audited inside {@link PFMSApiClient}. This class only
 * decides when a 401 retry must reuse the same audit {@code correlation_id}.
 * A later scheduler cycle always allocates a new UUID (new audit row).
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PFMSForwardingServiceImpl implements PFMSForwardingService {

    private final PFMSRepository pfmsRepository;
    private final PFMSApiClient pfmsApiClient;
    private final PFMSProperties pfmsProperties;

    private String currentToken;

    // function to get is the
    private boolean isRetryable(Exception e) {
        if (e instanceof FeignException feignException) {
            int status = feignException.status();
            // 5xx errors, 429 (Rate limit), 408 (Timeout) is retryable
            return status >= 500 || status == 429 || status == 408;
        }
        // Network drop / socket timeout
        if (e.getCause() instanceof SocketTimeoutException
                || e.getCause() instanceof ConnectException) {
            return true;
        }
        // 400, 404, 409, 422 permanent (Not retryable) will be REJECTED
        return false;
    }

    @Override
    public void forwardInitiatedTransactions(String triggeredBy) {
        int batchSize = pfmsProperties.getScheduler().getBatchSize();
        log.info("Pfms Daily Scheduler starting full processing for INITIATED transactions with batchSize={}", batchSize);
        while (true) {
            List<String> ids = pfmsRepository.fetchInitiatedTransactionIds(batchSize);
            if (ids == null || ids.isEmpty()) {
                log.info("Pfms Daily Scheduler: No more INITIATED transactions found. Processing complete.");
                break;
            }
            boolean batchProcessed = processBatch(ids, SchedulerType.DAILY, triggeredBy);
            if (!batchProcessed) {
                log.info("Pfms Daily Scheduler: Stopping further batches due to error.");
                break;
            }
        }
    }

    @Override
    public void retryFailedTransactions(String triggeredBy) {
        int batchSize = pfmsProperties.getScheduler().getBatchSize();
        log.info("Pfms Retry Scheduler starting full processing for FAILED transactions with batchSize={}", batchSize);

        Set<String> alreadyAttemptedIds = new HashSet<>();
        int iteration = 0;

        // Loop chalega max 4-5 batches (Constants.MAX_RETRY_BATCH_LIMIT)
        while (iteration < Constants.MAX_RETRY_BATCH_LIMIT) {
            iteration++;

            List<String> ids = pfmsRepository.fetchFailedTransactionIds(batchSize);
            if (ids == null || ids.isEmpty()) {
                log.info("Pfms Retry Scheduler: No FAILED transactions found. Processing complete.");
                break;
            }

            // Sirf wahi IDs lo jo is cycle me abhi tak attempt nahi huye
            List<String> newIds = ids.stream()
                    .filter(id -> !alreadyAttemptedIds.contains(id))
                    .toList();

            // Agar koi naya transaction nahi bacha (yaani sab attempt ho chuke hain)
            if (newIds.isEmpty()) {
                log.info("Pfms Retry Scheduler: All pending failed transactions have been retried for this cycle.");
                break;
            }

            alreadyAttemptedIds.addAll(newIds);
            log.info("Pfms Retry Scheduler: Processing batch #{} with {} transactions", iteration, newIds.size());

            boolean batchProcessed = processBatch(newIds, SchedulerType.RETRY, triggeredBy);
            if (!batchProcessed) {
                log.warn("Pfms Retry Scheduler: Stopping further batches due to error (e.g. Auth failure).");
                break;
            }
        }

        log.info("Pfms Retry Scheduler cycle finished. Total batches run: {}, Total records touched: {}",
                iteration, alreadyAttemptedIds.size());
    }

    private boolean processBatch(List<String> ids, SchedulerType type, String triggeredBy) {
        log.info("Pfms Scheduler [{}] Starting cycle", type);

        if (ids.isEmpty()) {
            log.info("Pfms Scheduler [{}] No transactions found. Skipping.", type);
            return false;
        }

        log.info("Pfms Scheduler [{}] Found {} transaction(s)", type, ids.size());
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            currentToken = pfmsApiClient.fetchAccessToken();
        } catch (Exception e) {
            log.error("Pfms Scheduler [{}] Auth failed, aborting cycle", type, e);
            return false;
        }

        int success = 0, failed = 0;
        for (String id : ids) {
            try {
                PfmsTransactionEntity transaction = pfmsRepository.fetchTransactionById(id);
                pfmsRepository.updateTransactionStatus(id, Constants.PROCESSING, null);
                log.info("Pfms [{}] Processing | id={} voucherNumber={}", type, id, transaction.getVoucherNumber());

                String pfmsResponse = pushWithTokenRefreshOnExpiry(transaction);

                pfmsRepository.updateTransactionStatus(id, Constants.SUCCESS, LocalDateTime.now());
                log.info("Pfms [{}] Success | id={} response={}", type, id, pfmsResponse);
                success++;

            } catch (Exception e) {
//                log.error("Pfms[{}] Failed | id={}", type, id, e);
//                pfmsRepository.updateTransactionStatus(id, Constants.FAILED, LocalDateTime.now());
//                failed++;
                if (isRetryable(e)) {
                    // Temporary issue -> Marking FAILED, so that retry sceduler will pick
                    pfmsRepository.updateTransactionStatus(id, Constants.FAILED, LocalDateTime.now());
                } else {
                    // Permanent data error (400, 422) -> Marking REJECTED
                    pfmsRepository.updateTransactionStatus(id, Constants.REJECTED, LocalDateTime.now());
                }
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
                .createdBy(triggeredBy)
                .build());

        return true;
    }

    /**
     * First push uses retryCount 0. On 401 the same {@code auditCorrelationId} is reused with retryCount 1
     * so persister updates one {@code pfms-data-push} row instead of inserting a second.
     */
    private String pushWithTokenRefreshOnExpiry(PfmsTransactionEntity transaction) {
        String auditCorrelationId = UUID.randomUUID().toString();
        try {
            return pfmsApiClient.pushTransaction(transaction, currentToken, auditCorrelationId, 0);
        } catch (Exception e) {
            if (!isUnauthorized(e)) {
                throw e;
            }
            log.warn("Pfms Token expired (401), refreshing | voucherNumber={}", transaction.getVoucherNumber());
            currentToken = pfmsApiClient.fetchAccessToken();
            return pfmsApiClient.pushTransaction(transaction, currentToken, auditCorrelationId, 1);
        }
    }

    private boolean isUnauthorized(Exception exception) {
        if (exception instanceof HttpClientErrorException.Unauthorized) {
            return true;
        }
        return exception instanceof FeignException feignException && feignException.status() == 401;
    }
}
