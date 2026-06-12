package upyog.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import upyog.config.EstateConfiguration;
import upyog.config.ServiceConstants;
import upyog.repository.DemandRepository;
import upyog.repository.EstateRepository;
import upyog.util.MdmsUtil;
import upyog.web.models.billing.BillingUser;
import upyog.web.models.billing.Demand;
import upyog.web.models.billing.DemandDetail;
import upyog.web.models.AllotmentRequest;
import upyog.web.models.Allotment;
import upyog.web.models.SchedulerLog;

import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DemandService {

    @Autowired
    private EstateConfiguration config;

    @Autowired
    private CalculationService calculationService;

    @Autowired
    private DemandRepository demandRepository;

    @Autowired
    private EstateRepository estateRepository;

    @Autowired
    private MdmsUtil mdmsUtil;

    private static final BigDecimal PENALTY_RATE = new BigDecimal("0.05");


    /**
     * Full revised flow (architecture diagram):
     * 1. Compute billing amount: full month or pro-rata if allotment ends this month.
     * 2. Check for unpaid previous demand.
     *    - If found: cancel it, apply 5% penalty on (prevUnpaid + current).
     *    - If not found: use current amount as-is.
     * 3. Save demand + publish to Kafka.
     */
    public void generateMonthlyDemand(RequestInfo requestInfo, Allotment allotment, LocalDate billingDate) {
        LocalDate endDate = allotment.getAgreementEndDate();
        BigDecimal monthlyRent = allotment.getMonthlyRent();
        YearMonth billingMonth = YearMonth.from(billingDate);

        // Step 1: Full month or pro-rata?
        boolean endsThisMonth = endDate != null
                && endDate.getMonthValue() == billingDate.getMonthValue()
                && endDate.getYear() == billingDate.getYear();

        LocalDate periodEnd;
        BigDecimal currentAmount;
        if (endsThisMonth) {
            int daysToCharge = endDate.getDayOfMonth();
            int daysInMonth = billingMonth.lengthOfMonth();
            currentAmount = monthlyRent
                    .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(daysToCharge));
            periodEnd = endDate;
            log.info("Pro-rata for allotment {}: {} days, amount {}", allotment.getAllotmentId(), daysToCharge, currentAmount);
        } else {
            currentAmount = monthlyRent;
            periodEnd = billingDate.withDayOfMonth(billingMonth.lengthOfMonth());
        }

        // Step 2: Check unpaid previous demand
        List<Demand> unpaidDemands = demandRepository.searchDemand(
                requestInfo, allotment.getTenantId(), allotment.getAssetNo(), config.getBusinessServiceName());

        BigDecimal finalAmount;
        List<Demand> toUpdate = Collections.emptyList();

        if (!unpaidDemands.isEmpty()) {
            // Cancel previous bills and accumulate unpaid amount
            BigDecimal prevUnpaid = unpaidDemands.stream()
                    .flatMap(d -> d.getDemandDetails().stream())
                    .map(DemandDetail::getTaxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 5% penalty on (prevUnpaid + current)
            BigDecimal base = prevUnpaid.add(currentAmount);
            finalAmount = base.add(base.multiply(PENALTY_RATE)).setScale(2, RoundingMode.HALF_UP);
            log.info("Penalty applied for allotment {}: prev={}, current={}, total={}",
                    allotment.getAllotmentId(), prevUnpaid, currentAmount, finalAmount);

            // Cancel (zero out) old demands
            unpaidDemands.forEach(d -> d.getDemandDetails().forEach(dd -> dd.setTaxAmount(BigDecimal.ZERO)));
            toUpdate = unpaidDemands;
        } else {
            finalAmount = currentAmount;
        }

        // Step 3: Build and save new demand
        BillingUser payer = BillingUser.builder()
                .name(allotment.getAlloteeName())
                .emailId(allotment.getEmailId())
                .mobileNumber(allotment.getMobileNo())
                .build();

        DemandDetail detail = DemandDetail.builder()
                .taxHeadMasterCode(ServiceConstants.EST_MONTHLY_RENT)
                .taxAmount(finalAmount)
                .collectionAmount(BigDecimal.ZERO)
                .tenantId(allotment.getTenantId())
                .build();

        Demand demand = Demand.builder()
                .consumerCode(allotment.getAssetNo())
                .demandDetails(List.of(detail))
                .payer(payer)
                .tenantId(allotment.getTenantId())
                .taxPeriodFrom(convertToTimestamp(billingDate))
                .taxPeriodTo(convertToTimestamp(periodEnd))
                .consumerType(config.getModuleName())
                .businessService(config.getBusinessServiceName())
                .build();

        if (!toUpdate.isEmpty()) {
            demandRepository.updateDemand(requestInfo, toUpdate);
        }
        // Save demand to billing service
        demandRepository.saveDemand(requestInfo, List.of(demand));

        // Save scheduler log entry to DB via Kafka
        String userUuid = requestInfo.getUserInfo() != null ? requestInfo.getUserInfo().getUuid() : "system";
        long now = System.currentTimeMillis();
        SchedulerLog schedulerLog = SchedulerLog.builder()
                .id(UUID.randomUUID().toString())
                .allotmentId(allotment.getAllotmentId())
                .tenantId(allotment.getTenantId())
                .billingDate(billingDate)
                .billingPeriodFrom(convertToTimestamp(billingDate))
                .billingPeriodTo(convertToTimestamp(periodEnd))
                .amount(finalAmount)
                .penaltyAmount(toUpdate.isEmpty() ? BigDecimal.ZERO : finalAmount.subtract(currentAmount))
                .paymentType(endsThisMonth ? "PARTIAL" : "FULL")
                .status("PENDING")
                .createdBy(userUuid)
                .createdTime(now)
                .lastModifiedBy(userUuid)
                .lastModifiedTime(now)
                .build();
        estateRepository.save(config.getSchedulerLogTopic(), Map.of("schedulerLog", schedulerLog));

        log.info("Demand saved and scheduler log published for allotment {}, period {} to {}",
                allotment.getAllotmentId(), billingDate, periodEnd);
    }

    public List<Demand> createDemand(AllotmentRequest allotmentRequest, Object mdmsData, boolean generateDemand) {
        String tenantId = allotmentRequest.getAllotments().get(0).getTenantId();
        String consumerCode = allotmentRequest.getAllotments().get(0).getAssetNo();
        Allotment allotment = allotmentRequest.getAllotments().get(0);
        User user = allotmentRequest.getRequestInfo().getUserInfo();
        log.info("user-details::" + user);

        BillingUser owner = BillingUser.builder()
                .name(allotment.getAlloteeName())
                .emailId(allotment.getEmailId())
                .mobileNumber(allotment.getMobileNo())
                .tenantId(tenantId)
                .build();

        Map<String, Object> mdmsDataMap = (Map<String, Object>) mdmsData;
        List<Map<String, Object>> taxRateList = (List<Map<String, Object>>)
                ((Map<String, Object>) ((Map<String, Object>) mdmsDataMap
                        .get("MdmsRes")).get("Estate")).get("EstateCalculationType");

        List<DemandDetail> demandDetails = calculationService.calculateDemand(allotmentRequest, taxRateList);

        LocalDate agreementStartDate = allotment.getAgreementStartDate();
        // taxPeriodTo = end of the first billing month only, not the full agreement end date
        LocalDate firstMonthEnd = agreementStartDate.withDayOfMonth(
                YearMonth.from(agreementStartDate).lengthOfMonth());

        Demand demand = Demand.builder()
                .consumerCode(consumerCode)
                .demandDetails(demandDetails)
                .payer(owner)
                .tenantId(tenantId)
                .taxPeriodFrom(convertToTimestamp(agreementStartDate))
                .taxPeriodTo(convertToTimestamp(firstMonthEnd))
                .consumerType(config.getModuleName())
                .businessService(config.getBusinessServiceName())
                .build();

        List<Demand> demands = new ArrayList<>();
        demands.add(demand);

        if (!generateDemand) {
            BigDecimal totalAmount = demandDetails.stream()
                    .map(DemandDetail::getTaxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            demand.setAdditionalDetails(totalAmount);
            return demands;
        }

        log.info("Sending call to billing service for generating demand for allotment id: " + consumerCode);
        return demandRepository.saveDemand(allotmentRequest.getRequestInfo(), demands);
    }

    private Long convertToTimestamp(LocalDate date) {
        return date.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

}
