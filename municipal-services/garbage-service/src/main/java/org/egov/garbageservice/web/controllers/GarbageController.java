package org.egov.garbageservice.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.egov.garbageservice.service.GarbageService;
import org.egov.garbageservice.service.Scheduler;
import org.egov.garbageservice.util.RequestInfoWrapper;
import org.egov.garbageservice.web.models.garbage.*;
import org.egov.garbageservice.web.models.billing.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for UPYOG Garbage Service.
 * Implements endpoints specified in "Garbage Service: Final Design (UPYOG)" Section 10,
 * with each @PostMapping mapped to its single canonical endpoint URL.
 */
@Slf4j
@Tag(name = "Garbage Service", description = "APIs for managing garbage collection applications and connection lifecycle")
@RestController
public class GarbageController {

    private final GarbageService garbageService;
    private final Scheduler scheduler;

    @Autowired
    public GarbageController(GarbageService garbageService, Scheduler scheduler) {
        this.garbageService = garbageService;
        this.scheduler = scheduler;
    }

    /**
     * Creates owner and tenant garbage applications.
     */
    @Operation(summary = "Create garbage application(s)")
    @PostMapping("/garbage/v1/_create")
    public ResponseEntity<GarbageResponse> create(@RequestBody @Valid GarbageRequest request) {
        log.info("REST request to create garbage application(s): {}", request);
        GarbageResponse response = garbageService.create(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates garbage application (workflow actions, verification, approval, edit).
     */
    @Operation(summary = "Update garbage application")
    @PostMapping("/garbage/v1/_update")
    public ResponseEntity<GarbageResponse> update(@RequestBody @Valid GarbageRequest request) {
        log.info("REST request to update garbage application(s): {}", request);
        GarbageResponse response = garbageService.update(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Multi-persona search for garbage applications (Owner cross-view, Tenant floor view, Employee search).
     */
    @Operation(summary = "Search garbage applications")
    @PostMapping("/garbage/v1/_search")
    public ResponseEntity<GarbageResponse> search(
            @RequestBody(required = false) GarbageSearchRequest searchRequest,
            @ModelAttribute GarbageSearchCriteria criteria) {
        log.info("REST request to search garbage applications. Body: {}, Params: {}", searchRequest, criteria);

        GarbageSearchCriteria resolvedCriteria = (searchRequest != null && searchRequest.getSearchCriteria() != null)
                ? searchRequest.getSearchCriteria()
                : (criteria != null ? criteria : new GarbageSearchCriteria());

        org.egov.common.contract.request.RequestInfo requestInfo = (searchRequest != null)
                ? searchRequest.getRequestInfo()
                : null;

        GarbageResponse response = garbageService.search(resolvedCriteria, requestInfo);
        return ResponseEntity.ok(response);
    }

    /**
     * Direct temporary disconnection of connections (no workflow required).
     */
    @Operation(summary = "Direct temporary disconnection")
    @PostMapping("/garbage/connection/_disconnect")
    public ResponseEntity<GarbageConnectionResponse> disconnect(@RequestBody @Valid GarbageConnectionRequest request) {
        log.info("REST request for direct connection disconnect: {}", request);
        GarbageConnectionResponse response = garbageService.disconnect(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Initiates connection reconnection request.
     */
    @Operation(summary = "Initiate connection reconnection")
    @PostMapping("/garbage/connection/_reconnect")
    public ResponseEntity<GarbageConnectionResponse> reconnect(@RequestBody @Valid GarbageConnectionRequest request) {
        log.info("REST request for connection reconnect: {}", request);
        GarbageConnectionResponse response = garbageService.reconnect(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Initiates permanent connection disconnection request.
     */
    @Operation(summary = "Initiate permanent disconnection")
    @PostMapping("/garbage/connection/_permanent")
    public ResponseEntity<GarbageConnectionResponse> permanentDisconnect(@RequestBody @Valid GarbageConnectionRequest request) {
        log.info("REST request for permanent connection disconnect: {}", request);
        GarbageConnectionResponse response = garbageService.permanentDisconnect(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reconnection or permanent disconnection workflow actions (verification / approval).
     */
    @Operation(summary = "Update connection lifecycle workflow")
    @PostMapping("/garbage/connection/_update")
    public ResponseEntity<GarbageConnectionResponse> updateConnectionWorkflow(@RequestBody @Valid GarbageConnectionRequest request) {
        log.info("REST request to update connection workflow: {}", request);
        GarbageConnectionResponse response = garbageService.updateConnection(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Searches connection lifecycle history.
     */
    @Operation(summary = "Search connection history")
    @PostMapping("/garbage/connection/_history")
    public ResponseEntity<List<GarbageConnectionLog>> searchConnectionHistory(
            @RequestParam(required = false) String propertyId,
            @RequestParam(required = false) String applicationNo,
            @RequestBody(required = false) RequestInfoWrapper requestInfoWrapper) {
        log.info("REST request for connection history: propertyId={}, applicationNo={}", propertyId, applicationNo);
        org.egov.common.contract.request.RequestInfo requestInfo = requestInfoWrapper != null ? requestInfoWrapper.getRequestInfo() : null;
        List<GarbageConnectionLog> logs = garbageService.searchConnectionHistory(propertyId, applicationNo, requestInfo);
        return ResponseEntity.ok(logs);
    }

    /**
     * Manually triggers the billing scheduler for a specific date.
     */
    @Operation(summary = "Trigger billing scheduler")
    @PostMapping("/garbage/v1/_trigger")
    public ResponseEntity<String> triggerScheduler(@RequestBody @Valid SchedulerRequest request) {
        log.info("REST request to manually trigger billing scheduler for date: {}", request.getBillingDate());
        String result = scheduler.triggerManually(request.getRequestInfo(), request.getBillingDate());
        return ResponseEntity.ok(result);
    }
}
