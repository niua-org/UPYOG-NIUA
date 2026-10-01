package org.egov.dx.upyogdata.pfms.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egov.dx.upyogdata.pfms.service.PFMSForwardingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PFMSRetryScheduler {

    private final PFMSForwardingService pfmsForwardingService;

    @Scheduled(cron = "${pfms.retry.scheduler.cron:0 0 5 * * *}")
    public void run() {
        log.info("Pfms Retry Scheduler Triggered");
        pfmsForwardingService.retryFailedTransactions("SCHEDULER");
    }
}
