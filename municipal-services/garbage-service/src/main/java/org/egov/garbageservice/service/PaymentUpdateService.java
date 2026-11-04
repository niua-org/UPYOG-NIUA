package org.egov.garbageservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import digit.models.coremodels.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.egov.garbageservice.config.GarbageServiceConfig;
import org.egov.garbageservice.web.models.GarbageAccount;
import org.egov.garbageservice.web.models.GarbageAccountRequest;
import org.egov.garbageservice.web.models.GarbagePaymentDetails;
import org.egov.garbageservice.web.models.SearchCriteriaGarbageAccount;
import org.egov.garbageservice.web.models.workflow.ProcessInstance;
import org.egov.garbageservice.web.models.workflow.ProcessInstanceRequest;
import org.egov.garbageservice.web.models.workflow.WorkflowService;
import org.egov.garbageservice.repository.GarbageAccountRepository;
import org.egov.garbageservice.util.GrbgConstants;
import org.egov.garbageservice.util.GrbgUtils;
import org.egov.garbageservice.util.ServiceConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer service responsible for intercepting external payment events and updating
 * the internal state of garbage accounts accordingly.
 *
 * <p>When a payment is made on a garbage account, this service handles parsing the payment payload,
 * updating the local {@code STATUS_PAID} state, generating audit logs, and persisting the resulting
 * transaction details to the database via Kafka topics.
 */
@Service
@Slf4j
public class PaymentUpdateService {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private GarbageServiceConfig config;

    @Autowired
    private GarbageAccountRepository garbageAccountRepository;

    @Autowired
    private WorkflowService workflowService;

    /**
     * Processes payment events received from the payment consumer and
     * creates rent payment entries for Estate Management payments.
     *
     * @param record payment event payload
     * @param topic  kafka topic name
     * @throws JsonProcessingException if payment payload conversion fails
     */
    public void process(HashMap<String, Object> record, String topic) throws JsonProcessingException {
        try {
            PaymentRequest paymentRequest = mapper.convertValue(record, PaymentRequest.class);
            process(paymentRequest, topic);
        } catch (Exception e) {
            log.error("Error processing payment update for Garbage: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing payment: " + e.getMessage(), e);
        }
    }

    /**
     * Processes payment request directly.
     *
     * @param paymentRequest payment request
     * @param topic          source topic or trigger identifier
     */
    public void process(PaymentRequest paymentRequest, String topic) {
        try {
            String businessService = paymentRequest.getPayment().getPaymentDetails().get(0).getBusinessService();

            if (!config.getBusinessService().equals(businessService)) {
                log.debug("Ignoring payment for businessService: {}", businessService);
                return;
            }

            String consumerCode = paymentRequest.getPayment().getPaymentDetails().get(0).getBill().getConsumerCode();
            BigDecimal paidAmount = paymentRequest.getPayment().getPaymentDetails().get(0).getTotalAmountPaid();
            String tenantId = paymentRequest.getPayment().getTenantId();
            String userUuid = paymentRequest.getRequestInfo() != null && paymentRequest.getRequestInfo().getUserInfo() != null
                    ? paymentRequest.getRequestInfo().getUserInfo().getUuid() : ServiceConstants.STATUS_SYSTEM;
            long now = Instant.now().toEpochMilli();
            LocalDate today = LocalDate.now();

            log.info("Processing payment for consumerCode: {}, amount: {}", consumerCode, paidAmount);

            List<String> consumerCodes = List.of(consumerCode);
            SearchCriteriaGarbageAccount searchCriteria = new SearchCriteriaGarbageAccount();
            searchCriteria.setApplicationNumber(consumerCodes);
            searchCriteria.setTenantId(tenantId);
            List<GarbageAccount> garbageAccounts = garbageAccountRepository.searchV2(searchCriteria);

            String garbageApplicationNo = consumerCode;
            Long garbageApplicationId = null;
            if (garbageAccounts != null && !garbageAccounts.isEmpty()) {
                garbageApplicationNo = garbageAccounts.get(0).getGrbgApplicationNumber();
                garbageApplicationId = garbageAccounts.get(0).getId();
            } else {
                log.error("GarbageAccount not found for consumerCode: {}", consumerCode);
            }

            GarbagePaymentDetails garbagePaymentDetails = GarbagePaymentDetails.builder()
                    .id(UUID.randomUUID().toString())
                    .applicationId(garbageApplicationId)
                    .applicationNo(garbageApplicationNo)
                    .penaltyAmount(BigDecimal.ZERO)
                    .rent(paidAmount)
                    .previousMonth(today.minusMonths(1).withDayOfMonth(1))
                    .paymentDate(today)
                    .lastDateOfPayment(today)
                    .duePaymentDate(today.plusMonths(1).withDayOfMonth(1))
                    .paymentStatus(ServiceConstants.STATUS_PAID)
                    .duePayment(BigDecimal.ZERO)
                    .validityDays(30)
                    .createdBy(userUuid)
                    .lastModifiedBy(userUuid)
                    .createdTime(now)
                    .lastModifiedTime(now)
                    .build();

            garbageAccountRepository.save(config.getMonthlyRentPaymentSaveTopic(), Map.of("garbagePaymentDetails", garbagePaymentDetails));
            log.info("GarbagePaymentDetails saved to ug_grbg_monthly_rent_payment for consumerCode: {}", consumerCode);

            // Update payment status to PAID and payment amount to ZERO
            try {
                if (garbageAccounts != null && !garbageAccounts.isEmpty()) {
                    GarbageAccount garbageAccount = garbageAccounts.get(0);
                    garbageAccount.setPaymentStatus(ServiceConstants.STATUS_PAID);
                    garbageAccount.setPaymentAmount(BigDecimal.ZERO);

                    // If a service disconnection was requested with disconnectionDate, activate DISCONNECTED status upon successful payment
                    boolean hasDisconnectionDate = garbageAccount.getAdditionalDetail() != null
                            && garbageAccount.getAdditionalDetail().has(GrbgConstants.DISCONNECTION_DATE)
                            && !garbageAccount.getAdditionalDetail().get(GrbgConstants.DISCONNECTION_DATE).isNull()
                            && StringUtils.isNotBlank(garbageAccount.getAdditionalDetail().get(GrbgConstants.DISCONNECTION_DATE).asText());

                    if (hasDisconnectionDate) {
                        garbageAccount.setStatus(GrbgConstants.SERVICE_STATUS_INACTIVE);
                        garbageAccount.setApplicationStatus(ServiceConstants.STATUS_DISCONNECTED);
                        if (garbageAccount.getGrbgApplication() != null) {
                            garbageAccount.getGrbgApplication().setStatus(ServiceConstants.STATUS_DISCONNECTED);
                        }
                        log.info("Activated DISCONNECTED status upon payment completion for Application No: {}", consumerCode);

                        // Trigger workflow DISCONNECT transition
                        try {
                            ProcessInstance processInstance = ProcessInstance.builder()
                                    .tenantId(garbageAccount.getTenantId())
                                    .businessService(config.getBusinessService())
                                    .moduleName(GrbgConstants.WORKFLOW_MODULE_NAME)
                                    .businessId(garbageApplicationNo)
                                    .action(GrbgConstants.WORKFLOW_ACTION_DISCONNECT)
                                    .comment("Service disconnected automatically on successful bill payment")
                                    .build();
                            ProcessInstanceRequest processInstanceRequest = ProcessInstanceRequest.builder()
                                    .requestInfo(paymentRequest.getRequestInfo())
                                    .processInstances(List.of(processInstance))
                                    .build();
                            workflowService.callWf(processInstanceRequest);
                            log.info("Workflow DISCONNECT transition successful for Application No: {}", consumerCode);
                        } catch (Exception wfEx) {
                            log.error("Failed to trigger workflow DISCONNECT for Application No: {}: {}", consumerCode, wfEx.getMessage());
                        }
                    }

                    // Update last modified audit details
                    if (garbageAccount.getAuditDetails() != null) {
                        garbageAccount.getAuditDetails().setLastModifiedBy(userUuid);
                        garbageAccount.getAuditDetails().setLastModifiedTime(now);
                    } else {
                        garbageAccount.setAuditDetails(GrbgUtils.getAuditDetails(userUuid, false));
                    }

                    GarbageAccountRequest garbageAccountRequest = new GarbageAccountRequest(paymentRequest.getRequestInfo(), List.of(garbageAccount), false, false);
                    garbageAccountRepository.save(config.getUpdateGarbageAccountTopic(), garbageAccountRequest);
                    log.info("Updated Garbage Application payment status to PAID and payment amount to ZERO for Application No: {}", consumerCode);
                }
            } catch (Exception e) {
                log.error("Failed to update garbage application payment status on payment: {}", e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("Error processing payment update for Garbage: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing payment: " + e.getMessage(), e);
        }
    }
}
