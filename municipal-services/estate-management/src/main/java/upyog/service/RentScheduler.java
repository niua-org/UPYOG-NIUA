package upyog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import upyog.repository.DemandRepository;
import upyog.repository.EstateRepository;
import upyog.util.MdmsUtil;
import upyog.web.models.Allotment;
import upyog.web.models.AllotmentSearchCriteria;
import upyog.web.models.billing.Demand;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentScheduler {

    private final EstateRepository estateRepository;
    private final DemandService    demandService;
    private final DemandRepository demandRepository;
    private final upyog.config.EstateConfiguration estateConfiguration;
    private final MdmsUtil mdmsUtil;

    // ── Auto trigger: cron runs daily, processes only on 1st of month ─────────

    @Scheduled(cron = "${est.scheduler.cron}")
    @SchedulerLock(name = "RentScheduler_generateDemand", lockAtLeastFor = "PT10M", lockAtMostFor = "PT30M")
    public void generateDemands() {
        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() != 1) return;
        process(today, RequestInfo.builder().build());
    }

    // ── Manual trigger ────────────────────────────────────────────────────────

    public String triggerManually(RequestInfo requestInfo, LocalDate billingDate) {
        LocalDate date = (billingDate != null ? billingDate : LocalDate.now()).withDayOfMonth(1);
        log.info("Manual scheduler trigger for {}", date);
        return process(date, requestInfo);
    }

    // ── Core ──────────────────────────────────────────────────────────────────

    private String process(LocalDate billingDate, RequestInfo requestInfo) {
        List<Allotment> allotments = estateRepository.searchAllotments(new AllotmentSearchCriteria());
        if (allotments.isEmpty()) return "No allotments found";

        // Collect all active consumer codes
        List<Allotment> activeAllotments = allotments.stream()
                .filter(a -> isActive(a, billingDate))
                .collect(Collectors.toList());

        if (activeAllotments.isEmpty()) return "No active allotments found";

        // Bulk search: find consumer codes that already have demand in this billing period
        YearMonth billingMonth = YearMonth.from(billingDate);
        long periodFrom = billingDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long periodTo   = billingMonth.atEndOfMonth().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        String tenantId = activeAllotments.get(0).getTenantId();
        String allConsumerCodes = activeAllotments.stream()
                .map(Allotment::getAssetNo)
                .collect(Collectors.joining(","));

        List<Demand> existingDemands = demandRepository.searchDemandByPeriod(
                requestInfo, tenantId, allConsumerCodes, estateConfiguration.getBusinessServiceName(), periodFrom, periodTo);

        Set<String> alreadyGenerated = existingDemands.stream()
                .map(Demand::getConsumerCode)
                .collect(Collectors.toSet());

        log.info("Billing period {}: {} active allotments, {} already have demands",
                billingDate, activeAllotments.size(), alreadyGenerated.size());

        BigDecimal penaltyRate = getPenaltyRateFromMdms(requestInfo, tenantId);
        log.info("Penalty rate from MDMS: {}%", penaltyRate.multiply(BigDecimal.valueOf(100)));

        int generated = 0, skipped = 0;

        for (Allotment allotment : activeAllotments) {
            if (alreadyGenerated.contains(allotment.getAssetNo())) {
                log.info("Demand already exists for consumerCode {}, skipping", allotment.getAssetNo());
                skipped++;
                continue;
            }
            try {
                demandService.generateMonthlyDemand(requestInfo, allotment, billingDate, penaltyRate);
                generated++;
                log.info("Demand generated for allotment {}", allotment.getAllotmentId());
            } catch (Exception e) {
                log.error("Failed for allotment {}: {}", allotment.getAllotmentId(), e.getMessage(), e);
            }
        }

        String result = "Generated: " + generated + ", Skipped: " + skipped;
        log.info("Scheduler done — {}", result);
        return result;
    }

    /**
     * Active = agreementStartDate <= billingDate AND (no endDate OR endDate >= billingDate)
     */
    private boolean isActive(Allotment allotment, LocalDate billingDate) {
        if (allotment.getAgreementStartDate() == null
                || allotment.getAgreementStartDate().isAfter(billingDate)) {
            log.warn("Skipping allotment {} — startDate={} is null or after billingDate={}",
                    allotment.getAllotmentId(), allotment.getAgreementStartDate(), billingDate);
            return false;
        }
        if (allotment.getAgreementEndDate() != null
                && allotment.getAgreementEndDate().isBefore(billingDate)) {
            log.warn("Skipping allotment {} — endDate={} is before billingDate={}",
                    allotment.getAllotmentId(), allotment.getAgreementEndDate(), billingDate);
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal getPenaltyRateFromMdms(RequestInfo requestInfo, String tenantId) {
        try {
            Object mdmsData = mdmsUtil.mDMSCall(requestInfo, tenantId);
            Map<String, Object> mdmsMap = (Map<String, Object>) mdmsData;
            List<Map<String, Object>> penaltyList = (List<Map<String, Object>>)
                    ((Map<String, Object>) ((Map<String, Object>) mdmsMap
                            .get("MdmsRes")).get("Estate")).get("Penalty");
            if (penaltyList != null && !penaltyList.isEmpty()) {
                Object rate = penaltyList.get(0).get("rate");
                return new BigDecimal(rate.toString()).divide(BigDecimal.valueOf(100));
            }
        } catch (Exception e) {
            log.error("Failed to fetch penalty rate from MDMS, defaulting to 5%: {}", e.getMessage());
        }
        return new BigDecimal("0.05");
    }
}
