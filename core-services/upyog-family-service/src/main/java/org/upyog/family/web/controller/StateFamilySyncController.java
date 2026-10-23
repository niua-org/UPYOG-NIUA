package org.upyog.family.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.upyog.family.domain.service.StateFamilySyncService;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyResponse;
import org.upyog.family.web.contract.state.StateFamilyJitProvisionRequest;
import org.upyog.family.web.contract.state.StateFamilySyncRequest;
import org.upyog.family.web.contract.state.StateFamilySyncResponse;

import java.util.Collections;

/**
 * REST Controller exposing UC2 State Family Registry Integration (Wrapper Model) endpoints.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/state")
@Slf4j
@Tag(name = "State Family Integration (UC2)", description = "Endpoints for State Wrapper ingestion, Family Found display, and JIT member provisioning")
public class StateFamilySyncController {

    private final StateFamilySyncService stateFamilySyncService;
    private final ResponseInfoFactory responseInfoFactory;

    @Autowired
    public StateFamilySyncController(StateFamilySyncService stateFamilySyncService,
                                     ResponseInfoFactory responseInfoFactory) {
        this.stateFamilySyncService = stateFamilySyncService;
        this.responseInfoFactory = responseInfoFactory;
    }

    /**
     * Endpoint for State Wrapper microservice to ingest external State Family Registry data into UPYOG.
     * Generates a referenceId and stages the family unit for the UPYOG "Family Found" UI screen.
     *
     * @param request state family sync request payload
     * @return StateFamilySyncResponse containing referenceId and masked member profiles
     */
    @PostMapping("/_sync")
    @Operation(summary = "Ingest State Family & Members payload from State Wrapper (UC2)")
    public ResponseEntity<StateFamilySyncResponse> syncStateFamily(@Valid @RequestBody StateFamilySyncRequest request) {
        log.info("[STATE-SYNC-CONTROLLER] Received POST /v1/state/_sync for stateFamilyId: {}",
                request.getStateFamily() != null ? request.getStateFamily().getStateFamilyId() : "null");

        StateFamilySyncResponse response = stateFamilySyncService.syncStateFamily(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Endpoint for UPYOG "Family Found" UI screen to retrieve staged family details using referenceId or stateFamilyId.
     *
     * @param referenceId unique reference identifier generated during sync
     * @param stateFamilyId state household identifier
     * @param requestInfo UPYOG request context envelope
     * @return StateFamilySyncResponse with masked demographics
     */
    @PostMapping("/_search")
    @Operation(summary = "Search staged State Family details for 'Family Found' UI screen (UC2)")
    public ResponseEntity<StateFamilySyncResponse> searchStateFamily(
            @RequestParam(value = "referenceId", required = false) String referenceId,
            @RequestParam(value = "stateFamilyId", required = false) String stateFamilyId,
            @RequestBody(required = false) RequestInfo requestInfo) {
        log.info("[STATE-SYNC-CONTROLLER] Received /v1/state/_search: refId={}, stateFamilyId={}",
                referenceId, stateFamilyId);

        StateFamilySyncResponse response = stateFamilySyncService.getStateFamily(referenceId, stateFamilyId, requestInfo);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * GET alias for UI convenience when rendering the "Family Found" screen.
     */
    @GetMapping("/_details")
    @Operation(summary = "GET alias to fetch staged State Family details for UI screen (UC2)")
    public ResponseEntity<StateFamilySyncResponse> getStateFamilyDetails(
            @RequestParam(value = "referenceId", required = false) String referenceId,
            @RequestParam(value = "stateFamilyId", required = false) String stateFamilyId) {
        log.info("[STATE-SYNC-CONTROLLER] Received GET /v1/state/_details: refId={}, stateFamilyId={}",
                referenceId, stateFamilyId);

        StateFamilySyncResponse response = stateFamilySyncService.getStateFamily(referenceId, stateFamilyId, null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Internal JIT Provisioning endpoint invoked by egov-user upon Member OTP verification:
     * Checks if family exists (creates if absent), checks if member is mapped (maps if absent), and returns active Family.
     *
     * @param request JIT provisioning request with userUuid and stateFamilyId
     * @return standard FamilyResponse wrapping the active Family aggregate
     */
    @PostMapping("/_jitProvision")
    @Operation(summary = "JIT check and provisioning of Family and Member mapping (UC2)")
    public ResponseEntity<FamilyResponse> jitProvision(@Valid @RequestBody StateFamilyJitProvisionRequest request) {
        log.info("[STATE-SYNC-CONTROLLER] Received POST /v1/state/_jitProvision for stateFamilyId: {}, userUuid: {}",
                request.getStateFamilyId(), request.getUserUuid());

        Family family = stateFamilySyncService.jitProvisionFamilyAndMember(request);
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);

        FamilyResponse response = FamilyResponse.builder()
                .responseInfo(responseInfo)
                .families(Collections.singletonList(family))
                .totalCount(1)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
