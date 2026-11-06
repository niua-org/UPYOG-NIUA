package org.egov.garbageservice.service;

import org.egov.garbageservice.web.models.garbage.GarbageApplication;
import org.egov.garbageservice.web.models.garbage.GarbageRequest;
import org.egov.garbageservice.web.models.garbage.GarbageBillGenerate;
import org.egov.garbageservice.web.models.garbage.AuditDetails;
import org.egov.garbageservice.repository.GarbageRepository;

import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.garbageservice.config.GarbageServiceConfig;
import org.egov.garbageservice.web.models.billing.Demand;
import org.egov.garbageservice.web.models.billing.DemandDetail;
import org.egov.garbageservice.web.models.garbage.AmountCalculationResult;
import org.egov.garbageservice.kafka.Producer;
import org.egov.garbageservice.repository.DemandRepository;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.egov.garbageservice.util.GrbgConstants;
import org.egov.garbageservice.util.GrbgUtils;
import org.egov.garbageservice.util.MdmsUtil;
import org.egov.garbageservice.util.ServiceConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class DemandService {

    @Autowired
    private GarbageCalculationService calculationService;



    @Autowired
    private DemandRepository demandRepository;

    @Autowired
    private Producer producer;

    @Autowired
    private GarbageServiceConfig config;

    @Autowired
    private MdmsUtil mdmsUtil;

    @Autowired
    private GarbageRepository garbageRepository;

    /**
     * Generates a monthly billing demand for the specified garbage account.
     *
     * <p>The demand generation process follows these steps:
     * <ol>
     *   <li>Calculates the billing period for the current month.</li>
     *   <li>Checks if a demand already exists for the given period to prevent duplicates.</li>
     *   <li>Calculates the current month's fee, including applicable rebates, via {@link GarbageCalculationService}.</li>
     *   <li>Retrieves any previous unpaid demands to calculate cumulative rental fees and penalties.</li>
     *   <li>Assembles the {@link DemandDetail} elements (tax, penalty, and rebate).</li>
     *   <li>Saves the new demand and optionally updates the history of unpaid demands.</li>
     *   <li>Logs the generation event to Kafka for asynchronous tracking.</li>
     *   <li>Updates the garbage account with the new due date, pending payment status, archives previous disconnection history, and pushes the updated account to Kafka.</li>
     * </ol>
     *
     * @param requestInfo    the contextual information for the API request
     * @param garbageAccount the garbage account for which the demand is being generated
     * @param billingDate    the date for which the billing cycle applies
     */

    public void generateDemand(RequestInfo requestInfo,
                               GarbageApplication garbageApplication,
                               LocalDate billingDate) {

        String consumerCode = garbageApplication.getApplicationNo() != null ? garbageApplication.getApplicationNo() : garbageApplication.getGrbgApplicationNumber();

        LocalDate periodFrom = billingDate;
        LocalDate periodTo = billingDate.plusMonths(1).minusDays(1);

        String userUuid =
                requestInfo.getUserInfo() != null
                        ? requestInfo.getUserInfo().getUuid()
                        : ServiceConstants.STATUS_SYSTEM;

        long now = System.currentTimeMillis();

        List<Demand> existingDemands =
                demandRepository.searchAllDemands(
                        requestInfo,
                        garbageApplication.getTenantId(),
                        consumerCode,
                        config.getBusinessService(),
                        false);

        boolean alreadyGenerated =
                existingDemands.stream()
                        .anyMatch(d ->
                                d.getTaxPeriodFrom().equals(convertToTimestamp(periodFrom))
                                        && d.getTaxPeriodTo().equals(convertToTimestamp(periodTo)));

        if (alreadyGenerated) {
            log.info(
                    "Demand already exists for garbage account {}, period {} to {}",
                    consumerCode,
                    periodFrom,
                    periodTo);
            updateDisconnectionHistory(garbageApplication, userUuid, requestInfo);
            garbageApplication.setStatus(GrbgConstants.SERVICE_STATUS_ACTIVE);
            return;
        }

        AmountCalculationResult currentAmount =
                calculationService.calculateAmount(requestInfo, garbageApplication);

        log.info(
                "Generating demand for garbage account {}, period {} to {}, amount {}",
                consumerCode,
                periodFrom,
                periodTo,
                currentAmount.getPayableAmount()
        );

        List<Demand> unpaidDemands =
                demandRepository.searchAllDemands(
                        requestInfo,
                        garbageApplication.getTenantId(),
                        consumerCode,
                        config.getBusinessService(),
                        true);

        BigDecimal rentalFeeAmount;
        BigDecimal penaltyAmount = BigDecimal.ZERO;
        BigDecimal finalAmount;

        List<Demand> demandsToUpdate =
                Collections.emptyList();

        if (!unpaidDemands.isEmpty()) {

            BigDecimal previousUnpaid =
                    unpaidDemands.stream()
                            .flatMap(d -> d.getDemandDetails().stream())
                            .map(DemandDetail::getTaxAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            rentalFeeAmount =
                    previousUnpaid.add(currentAmount.getPayableAmount());

            BigDecimal penaltyRate = mdmsUtil.getPenaltyRate(requestInfo, garbageApplication.getTenantId());
            penaltyAmount =
                    previousUnpaid
                            .multiply(penaltyRate)
                            .setScale(0, RoundingMode.HALF_UP);

            finalAmount =
                    rentalFeeAmount.add(penaltyAmount .setScale(0, RoundingMode.HALF_UP));

            demandsToUpdate = unpaidDemands;

            log.info(
                    "Penalty applied. Previous unpaid={}, current={}, penalty={}, total={}",
                    previousUnpaid,
                    currentAmount,
                    penaltyAmount,
                    finalAmount
            );

        } else {
            finalAmount = currentAmount.getPayableAmount();
        }


        User payer = User.builder()
                .name(garbageApplication.getName())
                .emailId(garbageApplication.getEmailId())
                .mobileNumber(garbageApplication.getMobileNumber())
                .tenantId(garbageApplication.getTenantId())
                .build();

        List<DemandDetail> demandDetails =
                new LinkedList<>();

        demandDetails.add(
                DemandDetail.builder()
                        .taxHeadMasterCode(ServiceConstants.GRBG_TAX_HEAD_CODE)
                        .taxAmount(currentAmount.getTotalAmount().setScale(0, RoundingMode.HALF_UP))
                        .collectionAmount(BigDecimal.ZERO)
                        .tenantId(garbageApplication.getTenantId())
                        .build()
        );

        if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {

            demandDetails.add(
                    DemandDetail.builder()
                            .taxHeadMasterCode(ServiceConstants.GRBG_PENALTY_FEE)
                            .taxAmount(penaltyAmount.setScale(0, RoundingMode.HALF_UP))
                            .collectionAmount(BigDecimal.ZERO)
                            .tenantId(garbageApplication.getTenantId())
                            .build()
            );
        }

        if (currentAmount.getRebateAmount().compareTo(BigDecimal.ZERO) > 0) {
            demandDetails.add(
                    DemandDetail.builder()
                            .taxHeadMasterCode(ServiceConstants.GRBG_REBATE_FEE)
                            .taxAmount(currentAmount.getRebateAmount().negate().setScale(0, RoundingMode.HALF_UP))
                            .collectionAmount(BigDecimal.ZERO)
                            .tenantId(garbageApplication.getTenantId())
                            .build()
            );
        }

        Demand demand = Demand.builder()
                .consumerCode(consumerCode)
                .demandDetails(demandDetails)
                .payer(payer)
                .tenantId(garbageApplication.getTenantId())
                .taxPeriodFrom(convertToTimestamp(periodFrom))
                .taxPeriodTo(convertToTimestamp(periodTo))
                .consumerType(config.getModuleName())
                .businessService(config.getBusinessService())
                .build();

        if (!demandsToUpdate.isEmpty()) {
            demandRepository.updateDemand(
                    requestInfo,
                    demandsToUpdate);
        }

        demandRepository.saveDemand(
                requestInfo,
                Collections.singletonList(demand));

        GarbageBillGenerate billLog =
                GarbageBillGenerate.builder()
                        .id(UUID.randomUUID().toString())
                        .garbageId(consumerCode)
                        .tenantId(garbageApplication.getTenantId())
                        .billingDate(billingDate)
                        .billingPeriodFrom(convertToTimestamp(periodFrom))
                        .billingPeriodTo(convertToTimestamp(periodTo))
                        .amount(finalAmount)
                        .penaltyAmount(penaltyAmount != null ? penaltyAmount : BigDecimal.ZERO)
                        .paymentType(ServiceConstants.PAYMENT_TYPE_FULL)
                        .status(ServiceConstants.STATUS_PENDING)
                        .auditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).lastModifiedBy(userUuid).lastModifiedTime(now).build())
                        .build();

        garbageRepository.saveBillGenerate(billLog);

        /**
         * Post-demand account synchronization:
         * 1. Updates billing metadata on the account (due date, payment amount, and PENDING_FOR_PAYMENT status).
         * 2. If resuming from a disconnection, updates disconnection history array with reconnection date,
         *    clears active disconnection keys, and resets status to ACTIVE.
         * 3. Updates audit details and persists the updated GarbageAccount via Kafka.
         */
        try {
            // 1. Set billing attributes
            garbageApplication.setDueDate(periodTo);
            garbageApplication.setPaymentStatus(ServiceConstants.STATUS_PENDING_FOR_PAYMENT);
            garbageApplication.setPaymentAmount(finalAmount);
            garbageApplication.setStatus(GrbgConstants.SERVICE_STATUS_ACTIVE);

            // 2. If resuming from disconnection, update disconnection history and reactivate service status
            updateDisconnectionHistory(garbageApplication, userUuid, requestInfo);

            // 3. Update audit details and push application update to Kafka
            if (garbageApplication.getAuditDetails() != null) {
                garbageApplication.getAuditDetails().setLastModifiedBy(userUuid);
                garbageApplication.getAuditDetails().setLastModifiedTime(Instant.now().toEpochMilli());
            } else {
                garbageApplication.setAuditDetails(GrbgUtils.getAuditDetails(userUuid, false));
            }
            GarbageRequest garbageRequest = new GarbageRequest(requestInfo, List.of(garbageApplication));
            garbageRepository.updateGarbageApplication(garbageRequest);
            log.info("Updated Garbage Application due date to {}, payment status to PENDING_FOR_PAYMENT, payment amount to {} for Application No: {}", periodTo, finalAmount, consumerCode);
        } catch (Exception e) {
            log.error("Failed to update allotment due date, payment status and payment amount on demand generation: {}", e.getMessage(), e);
        }

        log.info(
                "Demand generated successfully for garbage account {}",
                consumerCode);
    }

    /**
     * Updates and archives the disconnection history log when a garbage account resumes billing or service.
     *
     * <p>Checks if active disconnection fields exist in {@code additionalDetail}. If found,
     * appends the disconnection cycle (disconnection date, reason, and reconnection date) into
     * the {@code disconnectionHistory} array, clears the active disconnection attributes, resets
     * the service status to {@code ACTIVE}, and persists the updated account state.
     *
     * @param garbageAccount the {@link GarbageAccount} whose disconnection history is being updated
     * @param userUuid       the UUID of the user triggering the update
     * @param requestInfo    the {@link RequestInfo} providing request metadata and user context
     */
    public void updateDisconnectionHistory(GarbageApplication garbageApplication, String userUuid, RequestInfo requestInfo) {
        try {
            if (garbageApplication != null && garbageApplication.getAdditionalDetail() != null && garbageApplication.getAdditionalDetail().isObject()) {
                ObjectNode addDetail = (ObjectNode) garbageApplication.getAdditionalDetail();
                String dDate = addDetail.hasNonNull(GrbgConstants.DISCONNECTION_DATE)
                        ? addDetail.get(GrbgConstants.DISCONNECTION_DATE).asText() : null;
                String dReason = addDetail.hasNonNull(GrbgConstants.DISCONNECTION_REASON)
                        ? addDetail.get(GrbgConstants.DISCONNECTION_REASON).asText() : null;

                if (StringUtils.isNotBlank(dDate)) {
                    ArrayNode history = addDetail.has(GrbgConstants.DISCONNECTION_HISTORY) && addDetail.get(GrbgConstants.DISCONNECTION_HISTORY).isArray()
                            ? (ArrayNode) addDetail.get(GrbgConstants.DISCONNECTION_HISTORY)
                            : addDetail.putArray(GrbgConstants.DISCONNECTION_HISTORY);
                    ObjectNode entry = history.addObject();
                    entry.put(GrbgConstants.DISCONNECTION_DATE, dDate.trim());
                    if (StringUtils.isNotBlank(dReason)) {
                        entry.put(GrbgConstants.DISCONNECTION_REASON, dReason.trim());
                    }
                    entry.put(GrbgConstants.RECONNECTION_DATE, LocalDate.now().toString());

                    addDetail.remove(GrbgConstants.DISCONNECTION_DATE);
                    addDetail.remove(GrbgConstants.DISCONNECTION_REASON);
                    garbageApplication.setStatus(GrbgConstants.SERVICE_STATUS_ACTIVE);

                    if (garbageApplication.getAuditDetails() != null) {
                        garbageApplication.getAuditDetails().setLastModifiedBy(userUuid);
                        garbageApplication.getAuditDetails().setLastModifiedTime(Instant.now().toEpochMilli());
                    } else {
                        garbageApplication.setAuditDetails(GrbgUtils.getAuditDetails(userUuid, false));
                    }
                    GarbageRequest garbageRequest = new GarbageRequest(requestInfo, List.of(garbageApplication));
                    garbageRepository.updateGarbageApplication(garbageRequest);
                    log.info("Updated disconnection history for Garbage Application: {}", garbageApplication.getApplicationNo());
                }
            }
        } catch (Exception e) {
            log.error("Failed to update disconnection history for Garbage Account {}: {}",
                    garbageApplication != null ? (garbageApplication.getApplicationNo() != null ? garbageApplication.getApplicationNo() : garbageApplication.getGrbgApplicationNumber()) : "null", e.getMessage(), e);
        }
    }

    /**
     * Converts a given {@link LocalDate} to its corresponding epoch timestamp in milliseconds.
     *
     * @param date the date to convert
     * @return the epoch timestamp in milliseconds
     */

    private Long convertToTimestamp(LocalDate date) {
        return date.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }



}
