package org.upyog.family.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.upyog.family.domain.service.FamilyService;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMergeRequest;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.FamilyResponse;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.upyog.family.web.contract.FamilySplitRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * REST Controller exposing UPYOG Family Registry management endpoints.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/family")
@Slf4j
@Tag(name = "UPYOG Family Registry", description = "Endpoints for registering, searching, updating, splitting and merging household units in UPYOG")
public class FamilyController {

    private final FamilyService familyService;
    private final ResponseInfoFactory responseInfoFactory;

    @Autowired
    public FamilyController(FamilyService familyService, ResponseInfoFactory responseInfoFactory) {
        this.familyService = familyService;
        this.responseInfoFactory = responseInfoFactory;
    }

    /**
     * Registers a new Family Unit with an assigned Head of Family and initial members in UPYOG.
     * Generates a sequential family identifier prefixed with UPYOG_FAMILY- and persists the household aggregate.
     *
     * @param request the family creation request envelope containing metadata, head of family, and members
     * @return response entity containing the newly created Family aggregate with HTTP 201 CREATED status
     */
    @PostMapping("/_create")
    @Operation(summary = "Register a new Family Unit with Head and Members in UPYOG")
    public ResponseEntity<FamilyResponse> createFamily(@Valid @RequestBody FamilyRequest request) {
        log.info("[UPYOG-FAMILY-CONTROLLER] Received /_create request for tenant: {}", request.getFamily().getTenantId());
        Family createdFamily = familyService.createFamily(request);
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);

        FamilyResponse response = FamilyResponse.builder()
                .responseInfo(responseInfo)
                .families(Collections.singletonList(createdFamily))
                .totalCount(1)
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Searches Family Units based on query filters (familyId, member UUID, mobile number, or tenantId) in UPYOG.
     * Enriches matched families with active member profiles from the user registry.
     *
     * @param criteria query criteria containing filter attributes
     * @param requestInfo UPYOG request context envelope
     * @return response entity containing the list of matching Family aggregates with HTTP 200 OK status
     */
    @PostMapping("/_search")
    @Operation(summary = "Search Family Units by FamilyId, Member UUID, Mobile Number, or Tenant in UPYOG")
    public ResponseEntity<FamilyResponse> searchFamilies(@Valid @ModelAttribute FamilySearchCriteria criteria,
                                                         @RequestBody(required = false) RequestInfo requestInfo) {
        log.info("[UPYOG-FAMILY-CONTROLLER] Received /_search request with criteria: tenant={}, familyId={}, memberUuid={}",
                criteria.getTenantId(), criteria.getFamilyId(), criteria.getMemberUserUuid());

        List<Family> families = familyService.searchFamilies(criteria, requestInfo);
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);

        FamilyResponse response = FamilyResponse.builder()
                .responseInfo(responseInfo)
                .families(families)
                .totalCount(families.size())
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Updates an existing Family Unit's metadata (category, head of family) and active members in UPYOG.
     * Records audit history and validates that the updated structure retains an active Head of Family.
     *
     * @param request the family update request envelope containing updated family attributes
     * @return response entity containing the updated Family aggregate with HTTP 200 OK status
     */
    @PostMapping("/_update")
    @Operation(summary = "Update an existing Family Unit (members addition/removal, HoF succession) in UPYOG")
    public ResponseEntity<FamilyResponse> updateFamily(@Valid @RequestBody FamilyRequest request) {
        log.info("[UPYOG-FAMILY-CONTROLLER] Received /_update request for family: {}", request.getFamily().getFamilyId());
        Family updatedFamily = familyService.updateFamily(request);
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);

        FamilyResponse response = FamilyResponse.builder()
                .responseInfo(responseInfo)
                .families(Collections.singletonList(updatedFamily))
                .totalCount(1)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Splits an existing Family Unit into a new Household Unit in UPYOG, recording full lineage history.
     * Moves specified members into a new family unit headed by the designated member.
     *
     * @param request the split request containing parent family ID, new head UUID, and splitting member UUIDs
     * @return response entity containing the newly formed child Family aggregate with HTTP 201 CREATED status
     */
    @PostMapping("/_split")
    @Operation(summary = "Split an existing Family Unit into a new Household Unit in UPYOG")
    public ResponseEntity<FamilyResponse> splitFamily(@Valid @RequestBody FamilySplitRequest request) {
        log.info("[UPYOG-FAMILY-CONTROLLER] Received /_split request for parent family: {}", request.getParentFamilyId());
        Family childFamily = familyService.splitFamily(request);
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);

        FamilyResponse response = FamilyResponse.builder()
                .responseInfo(responseInfo)
                .families(Collections.singletonList(childFamily))
                .totalCount(1)
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Merges two existing Family Units into a unified Household Unit in UPYOG.
     * Re-assigns members of the merging family to the surviving family and archives the merging unit with lineage tracking.
     *
     * @param request the merge request containing surviving family ID, merging family ID, and common head UUID
     * @return response entity containing the consolidated surviving Family aggregate with HTTP 200 OK status
     */
    @PostMapping("/_merge")
    @Operation(summary = "Merge two existing Family Units into a unified Household Unit in UPYOG")
    public ResponseEntity<FamilyResponse> mergeFamily(@Valid @RequestBody FamilyMergeRequest request) {
        log.info("[UPYOG-FAMILY-CONTROLLER] Received /_merge request: merging {} into {}",
                request.getMergingFamilyId(), request.getSurvivingFamilyId());
        Family survivingFamily = familyService.mergeFamily(request);
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);

        FamilyResponse response = FamilyResponse.builder()
                .responseInfo(responseInfo)
                .families(Collections.singletonList(survivingFamily))
                .totalCount(1)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
