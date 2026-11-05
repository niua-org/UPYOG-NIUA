package org.egov.garbageservice.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.garbageservice.config.GarbageServiceConfig;
import org.egov.garbageservice.kafka.Producer;
import org.egov.garbageservice.web.models.garbage.GarbageApplication;
import org.egov.garbageservice.web.models.garbage.GarbageRequest;
import org.egov.garbageservice.web.models.notification.SMSRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service dispatching transactional notifications for Garbage Service lifecycle events.
 * Implements SMS templates defined in HLD Section 16.
 */
@Service
@Slf4j
public class NotificationService {

    private final Producer producer;
    private final GarbageServiceConfig config;



    public NotificationService(Producer producer, GarbageServiceConfig config) {
        this.producer = producer;
        this.config = config;
    }

    /**
     * Sends SMS notifications upon application creation for owner and tenants.
     *
     * @param request the create request containing owner and tenant applications
     */
    public void processApplyNotification(GarbageRequest request) {
        if (request == null || request.getGarbageApplications() == null) {
            return;
        }

        for (GarbageApplication app : request.getGarbageApplications()) {
            try {
                String mobileNumber = app.getMobileNumber();
                String name = app.getName() != null ? app.getName() : "Citizen";
                String appNo = app.getApplicationNo();
                String propId = app.getPropertyId() != null ? app.getPropertyId() : "";

                if (StringUtils.isBlank(mobileNumber)) {
                    continue;
                }

                String message;
                if (Boolean.TRUE.equals(app.getIsOwner())) {
                    message = String.format("Dear %s, garbage application %s for property %s submitted successfully.",
                            name, appNo, propId);
                } else {
                    message = String.format("Dear %s, garbage collection application %s registered for your floor at property %s. Log in to view bill.",
                            name, appNo, propId);
                }

                SMSRequest smsRequest = SMSRequest.builder()
                        .mobileNumber(mobileNumber)
                        .message(message)
                        .category("TRANSACTION")
                        .build();

                producer.push(config.getNotificationSmsTopic(), smsRequest);
                log.info("Dispatched APPLY SMS to {} for application {}", mobileNumber, appNo);
            } catch (Exception e) {
                log.error("Failed to send APPLY notification for app {}: {}", app.getApplicationNo(), e.getMessage());
            }
        }
    }

    /**
     * Sends SMS notifications upon application approval.
     */
    public void processApprovalNotification(GarbageApplication app) {
        if (app == null || StringUtils.isBlank(app.getMobileNumber())) {
            return;
        }
        try {
            String message = String.format("Your garbage collection application %s is approved. Monthly collection service active.",
                    app.getApplicationNo());

            SMSRequest smsRequest = SMSRequest.builder()
                    .mobileNumber(app.getMobileNumber())
                    .message(message)
                    .category("TRANSACTION")
                    .build();

            producer.push(config.getNotificationSmsTopic(), smsRequest);
            log.info("Dispatched APPROVAL SMS to {} for application {}", app.getMobileNumber(), app.getApplicationNo());
        } catch (Exception e) {
            log.error("Failed to send APPROVE notification: {}", e.getMessage());
        }
    }

    /**
     * Sends SMS notifications for service disconnection.
     */
    public void processDisconnectNotification(GarbageApplication app, LocalDate effectiveDate) {
        if (app == null || StringUtils.isBlank(app.getMobileNumber())) {
            return;
        }
        try {
            String dateStr = effectiveDate != null ? effectiveDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "today";
            String message = String.format("Garbage collection service for property %s disconnected effective %s by owner.",
                    app.getPropertyId(), dateStr);

            SMSRequest smsRequest = SMSRequest.builder()
                    .mobileNumber(app.getMobileNumber())
                    .message(message)
                    .category("TRANSACTION")
                    .build();

            producer.push(config.getNotificationSmsTopic(), smsRequest);
            log.info("Dispatched DISCONNECT SMS to {}", app.getMobileNumber());
        } catch (Exception e) {
            log.error("Failed to send DISCONNECT notification: {}", e.getMessage());
        }
    }

    /**
     * Sends SMS notifications for service reconnection.
     */
    public void processReconnectNotification(GarbageApplication app) {
        if (app == null || StringUtils.isBlank(app.getMobileNumber())) {
            return;
        }
        try {
            String message = String.format("Garbage collection service for property %s reconnected successfully. Monthly billing resumed.",
                    app.getPropertyId());

            SMSRequest smsRequest = SMSRequest.builder()
                    .mobileNumber(app.getMobileNumber())
                    .message(message)
                    .category("TRANSACTION")
                    .build();

            producer.push(config.getNotificationSmsTopic(), smsRequest);
            log.info("Dispatched RECONNECT SMS to {}", app.getMobileNumber());
        } catch (Exception e) {
            log.error("Failed to send RECONNECT notification: {}", e.getMessage());
        }
    }

    /**
     * Sends SMS notifications for monthly bill generation.
     */
    public void processBillGeneratedNotification(GarbageApplication app, BigDecimal amount, String month) {
        if (app == null || StringUtils.isBlank(app.getMobileNumber())) {
            return;
        }
        try {
            String message = String.format("Garbage bill for %s of Rs %s is generated for %s. Pay online on UPYOG citizen portal.",
                    month, amount != null ? amount.toPlainString() : "0.00", app.getApplicationNo());

            SMSRequest smsRequest = SMSRequest.builder()
                    .mobileNumber(app.getMobileNumber())
                    .message(message)
                    .category("TRANSACTION")
                    .build();

            producer.push(config.getNotificationSmsTopic(), smsRequest);
            log.info("Dispatched BILL_GENERATED SMS to {}", app.getMobileNumber());
        } catch (Exception e) {
            log.error("Failed to send BILL_GENERATED notification: {}", e.getMessage());
        }
    }
}
