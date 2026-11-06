package org.egov.garbageservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.egov.common.contract.request.RequestInfo;
import org.egov.garbageservice.repository.GarbageRepository;
import org.egov.garbageservice.web.models.garbage.*;
import org.egov.garbageservice.web.models.user.*;
import org.egov.garbageservice.web.models.workflow.*;
import org.egov.garbageservice.web.models.billing.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Automated billing scheduler running nightly via ShedLock.
 * Halts recurring monthly billing demands for disconnected and permanently disconnected applications
 * as specified in HLD Section 5.3 & 15.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Scheduler {

    private final GarbageRepository garbageRepository;
    private final DemandService demandService;

    @Scheduled(cron = "${gc.scheduler.cron:${grbg.scheduler.cron:0 0 1 * * ?}}")
    @SchedulerLock(name = "GarbageScheduler_generateDemand", lockAtLeastFor = "PT10M", lockAtMostFor = "PT30M")
    public void generateDemands() {
        LocalDate today = LocalDate.now();
        log.info("Starting automated nightly garbage billing scheduler for date: {}", today);
        process(today, RequestInfo.builder().build());
    }

    public String triggerManually(RequestInfo requestInfo, LocalDate billingDate) {
        LocalDate date = (billingDate != null ? billingDate : LocalDate.now());
        log.info("Manual scheduler trigger for date: {}", date);
        return process(date, requestInfo);
    }

    private String process(LocalDate billingDate, RequestInfo requestInfo) {
        GarbageSearchCriteria criteria = GarbageSearchCriteria.builder()
                .status(List.of("APPROVED"))
                .connectionStatus("ACTIVE")
                .isSchedulerCall(true)
                .build();

        List<GarbageApplication> applications = garbageRepository.getGarbageApplications(criteria);

        if (applications.isEmpty()) {
            log.info("No active or approved garbage applications found for billing.");
            return "No garbage applications found with status APPROVED and connection_status ACTIVE";
        }

        // Section 2 & 8: Fetch status APPROVED and connection_status ACTIVE
        List<GarbageApplication> activeApps = applications.stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getConnectionStatus()) || org.apache.commons.lang3.StringUtils.isBlank(a.getConnectionStatus()))
                .filter(a -> !"DISCONNECTED".equalsIgnoreCase(a.getConnectionStatus()))
                .filter(a -> !"PERMANENTLY_DISCONNECTED".equalsIgnoreCase(a.getConnectionStatus()))
                .filter(a -> isBillingDue(a, billingDate))
                .collect(Collectors.toList());

        if (activeApps.isEmpty()) {
            log.info("No garbage applications due for billing on date: {}", billingDate);
            return "No active Garbage Applications due on " + billingDate;
        }

        int generated = 0;
        long periodFrom = billingDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long periodTo = billingDate.plusMonths(1).minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long now = System.currentTimeMillis();

        for (GarbageApplication app : activeApps) {
            try {
                demandService.generateDemand(requestInfo, app, billingDate);
                generated++;

                // Log execution to ug_gc_bill_generate (HLD Section 11.6)
                GarbageBillGenerate billLog = GarbageBillGenerate.builder()
                        .id(UUID.randomUUID().toString())
                        .garbageId(app.getApplicationNo())
                        .tenantId(app.getTenantId())
                        .billingDate(billingDate)
                        .billingPeriodFrom(periodFrom)
                        .billingPeriodTo(periodTo)
                        .amount(app.getPaymentAmount() != null ? app.getPaymentAmount() : BigDecimal.ZERO)
                        .penaltyAmount(BigDecimal.ZERO)
                        .paymentType("MONTHLY")
                        .status("GENERATED")
                        .auditDetails(AuditDetails.builder().createdBy("SCHEDULER").createdTime(now).build())
                        .build();

                garbageRepository.saveBillGenerate(billLog);
                log.info("Demand generated and logged in ug_gc_bill_generate for application: {}", app.getApplicationNo());

            } catch (Exception e) {
                log.error("Failed to generate demand for application {}: {}", app.getApplicationNo(), e.getMessage());
            }
        }

        String result = "Demands generated: " + generated;
        log.info("Billing scheduler execution finished: {}", result);
        return result;
    }

    private boolean isBillingDue(GarbageApplication application, LocalDate schedulerDate) {
        Long approvalTimestamp = application.getApprovalDate();
        if (approvalTimestamp == null) {
            // Default to due on 1st of month if approvalDate is null
            return schedulerDate.getDayOfMonth() == 1;
        }

        LocalDate approvalDate = Instant.ofEpochMilli(approvalTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        if (schedulerDate.isBefore(approvalDate)) {
            return false;
        }

        int billingDay = Math.min(approvalDate.getDayOfMonth(), schedulerDate.lengthOfMonth());
        return schedulerDate.getDayOfMonth() == billingDay;
    }
}
