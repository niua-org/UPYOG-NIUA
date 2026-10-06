package org.egov.dx.upyogdata.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.egov.dx.upyogdata.constants.Constants;
import org.egov.dx.upyogdata.pfms.models.PFMSCreateTransactionRequest;
import org.egov.dx.upyogdata.pfms.service.PFMSForwardingService;
import org.egov.dx.upyogdata.pfms.service.PFMSService;
import org.egov.externalaudit.constants.ExternalApiAuditConstants;
import org.egov.externalaudit.model.ExternalIntegrationContext;
import org.egov.externalaudit.service.ExternalApiAuditLogger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

/**
 * REST controller exposing APIs for PFMS transaction data exchange.
 *
 * Accepts transaction data from state systems and forwards the
 * validated request to PFMS Servers
 */
@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Upyog Data Sharing Service", description = "APIs for Upyog Data Sharing Services")
public class PFMSController {

    private static final String PFMS_TENANT = "PFMS";

    private final PFMSService pfmsService;
    private final PFMSForwardingService pfmsForwardingService;
    private final ExternalApiAuditLogger auditLogger;

    @PostMapping("/_create")
    @Operation(summary = "Create", description = "State will push data in this create")
    public ResponseEntity<?> createTransaction(
            @Valid @RequestBody PFMSCreateTransactionRequest request) {
        try {
            Object result = auditLogger.logInboundApi(ExternalIntegrationContext.builder()
                    .tenantId(PFMS_TENANT)
                    .externalApiName(ExternalApiAuditConstants.API_STATE_PFMS_TRANSACTION_CREATE)
                    .requestPayload(request)
                    .originatingCorrelationId(resolveOriginatingCorrelationId(request))
                    .httpMethod("POST")
                    .endpoint("/upyog-data-dx/v1/transactions/_create")
                    .build(), () -> pfmsService.createTransaction(request));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/scheduler/_trigger")
    @Operation(summary = "Manually trigger PFMS forwarding scheduler", description = "For testing only")
    public ResponseEntity<?> triggerScheduler() {
        pfmsForwardingService.forwardInitiatedTransactions(Constants.MANUAL);
        return ResponseEntity.ok("Scheduler triggered");
    }

    @PostMapping("/scheduler/_retryTrigger")
    @Operation(summary = "Manually trigger PFMS Retry scheduler for Failed Status", description = "For testing only")
    public ResponseEntity<?> triggerRetryScheduler() {
        pfmsForwardingService.retryFailedTransactions(Constants.MANUAL);
        return ResponseEntity.ok("Retry Scheduler triggered");
    }

    private String resolveOriginatingCorrelationId(PFMSCreateTransactionRequest request) {
        if (request.getRequestInfo() != null
                && request.getRequestInfo().getCorrelationId() != null
                && !request.getRequestInfo().getCorrelationId().isBlank()) {
            return request.getRequestInfo().getCorrelationId();
        }
        return null;
    }
}
