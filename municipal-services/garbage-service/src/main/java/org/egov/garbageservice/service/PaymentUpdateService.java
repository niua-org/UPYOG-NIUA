package org.egov.garbageservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import digit.models.coremodels.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.garbageservice.config.GarbageServiceConfig;
import org.egov.garbageservice.repository.GarbageRepository;
import org.egov.garbageservice.util.GrbgConstants;
import org.egov.garbageservice.web.models.garbage.*;
import org.egov.garbageservice.web.models.user.*;
import org.egov.garbageservice.web.models.workflow.*;
import org.egov.garbageservice.web.models.billing.*;
import org.egov.garbageservice.web.models.workflow.ProcessInstance;
import org.egov.garbageservice.web.models.workflow.ProcessInstanceRequest;
import org.egov.garbageservice.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Service processing incoming payment events from Kafka (egov.collection.payment-create).
 * Logs payments to ug_gc_monthly_rent_payment and updates application payment status.
 */
@Service
@Slf4j
public class PaymentUpdateService {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private GarbageServiceConfig config;

    @Autowired
    private GarbageRepository garbageRepository;

    @Autowired
    private WorkflowService workflowService;

    public void process(HashMap<String, Object> record, String topic) throws JsonProcessingException {
        try {
            PaymentRequest paymentRequest = mapper.convertValue(record, PaymentRequest.class);
            process(paymentRequest, topic);
        } catch (Exception e) {
            log.error("Error processing payment update for Garbage: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing payment: " + e.getMessage(), e);
        }
    }

    public void process(PaymentRequest paymentRequest, String topic) {
        try {
            if (paymentRequest == null || paymentRequest.getPayment() == null
                    || paymentRequest.getPayment().getPaymentDetails() == null
                    || paymentRequest.getPayment().getPaymentDetails().isEmpty()) {
                return;
            }

            String businessService = paymentRequest.getPayment().getPaymentDetails().get(0).getBusinessService();
            if (!config.getBusinessService().equals(businessService)) {
                log.debug("Ignoring payment for different businessService: {}", businessService);
                return;
            }

            String consumerCode = paymentRequest.getPayment().getPaymentDetails().get(0).getBill().getConsumerCode();
            BigDecimal paidAmount = paymentRequest.getPayment().getPaymentDetails().get(0).getTotalAmountPaid();
            String tenantId = paymentRequest.getPayment().getTenantId();
            String userUuid = paymentRequest.getRequestInfo() != null && paymentRequest.getRequestInfo().getUserInfo() != null
                    ? paymentRequest.getRequestInfo().getUserInfo().getUuid() : "SYSTEM";
            long now = Instant.now().toEpochMilli();
            LocalDate today = LocalDate.now();

            log.info("Processing garbage payment for consumerCode: {}, amount: {}", consumerCode, paidAmount);

            GarbageSearchCriteria searchCriteria = GarbageSearchCriteria.builder()
                    .tenantId(tenantId)
                    .applicationNumber(Collections.singletonList(consumerCode))
                    .build();

            List<GarbageApplication> applications = garbageRepository.getGarbageApplications(searchCriteria);

            String billNo = "BILL-" + System.currentTimeMillis();
            try {
                if (paymentRequest.getPayment().getPaymentDetails().get(0).getBill() != null
                        && paymentRequest.getPayment().getPaymentDetails().get(0).getBill().getBillNumber() != null) {
                    billNo = paymentRequest.getPayment().getPaymentDetails().get(0).getBill().getBillNumber();
                }
            } catch (Exception e) {
                log.debug("Unable to extract bill number: {}", e.getMessage());
            }

            Long appId = null;
            if (!applications.isEmpty()) {
                appId = applications.get(0).getId();
            }

            // Insert into ug_gc_monthly_rent_payment (HLD Section 11.5)
            GarbageMonthlyRentPayment rentPayment = GarbageMonthlyRentPayment.builder()
                    .id(UUID.randomUUID().toString())
                    .garbageId(appId != null ? appId : 0L)
                    .applicationNo(consumerCode)
                    .billNo(billNo)
                    .demandId(null)
                    .billDate(today)
                    .periodFrom(today.withDayOfMonth(1))
                    .periodTo(today.plusMonths(1).withDayOfMonth(1).minusDays(1))
                    .billAmount(paidAmount)
                    .paymentStatus("PAID")
                    .tenant(tenantId)
                    .auditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).build())
                    .build();

            garbageRepository.saveMonthlyBill(rentPayment);
            log.info("Logged payment to ug_gc_monthly_rent_payment for consumerCode: {}", consumerCode);

            // Update application status to PAID
            if (!applications.isEmpty()) {
                GarbageApplication app = applications.get(0);
                app.setPaymentStatus("PAID");
                app.setPaymentAmount(BigDecimal.ZERO);

                AuditDetails auditDetails = app.getAuditDetails() != null ? app.getAuditDetails() : AuditDetails.builder().build();
                auditDetails.setLastModifiedBy(userUuid);
                auditDetails.setLastModifiedTime(now);
                app.setAuditDetails(auditDetails);

                GarbageRequest garbageRequest = GarbageRequest.builder()
                        .requestInfo(paymentRequest.getRequestInfo())
                        .garbageApplications(Collections.singletonList(app))
                        .build();

                garbageRepository.updateGarbageApplication(garbageRequest);
                log.info("Updated Garbage Application payment status to PAID for application: {}", consumerCode);
            }

        } catch (Exception e) {
            log.error("Error processing payment update for Garbage: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing payment: " + e.getMessage(), e);
        }
    }
}
