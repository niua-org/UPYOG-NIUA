package org.upyog.family.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.upyog.family.domain.service.FamilyDashboardService;
import org.upyog.family.web.contract.FamilyDashboardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing family analytics, verification statistics, and household dashboard metrics in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/dashboard")
@Slf4j
@Validated
@Tag(name = "UPYOG Family Dashboard", description = "Endpoints for aggregated household metrics and demographic summaries")
public class FamilyDashboardController {

    private final FamilyDashboardService dashboardService;

    @Autowired
    public FamilyDashboardController(FamilyDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Retrieves aggregated household dashboard analytics for a given family unit in UPYOG.
     * Computes active member counts, identity verification ratio, attached document counts, and lineage events.
     *
     * @param familyId the unique family identifier
     * @param tenantId the tenant identifier
     * @param requestInfo UPYOG request context envelope
     * @return response entity containing aggregated FamilyDashboardResponse metrics with HTTP 200 OK status
     */
    @PostMapping("/_search")
    @Operation(summary = "Retrieve aggregated household metrics and verification statistics in UPYOG")
    public ResponseEntity<FamilyDashboardResponse> getDashboard(
            @NotBlank(message = "Family ID is mandatory") @RequestParam("familyId") String familyId,
            @NotBlank(message = "Tenant ID is mandatory") @RequestParam("tenantId") String tenantId,
            @RequestBody(required = false) RequestInfo requestInfo) {

        log.info("[UPYOG-DASHBOARD-CONTROLLER] Received /_search request for family: {}, tenant: {}", familyId, tenantId);
        FamilyDashboardResponse response = dashboardService.getFamilyDashboard(familyId, tenantId, requestInfo);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
