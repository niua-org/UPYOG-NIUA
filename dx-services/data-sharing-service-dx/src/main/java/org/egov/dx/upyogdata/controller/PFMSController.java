package org.egov.dx.upyogdata.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.egov.dx.upyogdata.pfms.service.PFMSForwardingService;
import org.egov.dx.upyogdata.pfms.service.PFMSService;
import org.egov.dx.upyogdata.pfms.models.PFMSCreateTransactionRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    private final PFMSService pfmsService;
    private final PFMSForwardingService pfmsForwardingService;

    @PostMapping("/_create")
    @Operation(summary = "Create", description = "State will push data in this create")
    public ResponseEntity<?> createTransaction(
            @Valid @RequestBody PFMSCreateTransactionRequest request) {
        try {
            return ResponseEntity.ok(pfmsService.createTransaction(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    @PostMapping("/scheduler/_trigger")
    @Operation(summary = "Manually trigger PFMS forwarding scheduler", description = "For testing only")
    public ResponseEntity<?> triggerScheduler() {
        pfmsForwardingService.forwardInitiatedTransactions("MANUAL");
        return ResponseEntity.ok("Scheduler triggered");
    }

    @PostMapping("/scheduler/_retryTrigger")
    @Operation(summary = "Manually trigger PFMS Retry scheduler for Failed Status", description = "For testing only")
    public ResponseEntity<?> triggerRetryScheduler() {
        pfmsForwardingService.retryFailedTransactions("MANUAL");
        return ResponseEntity.ok("Retry Scheduler triggered");
    }


}
