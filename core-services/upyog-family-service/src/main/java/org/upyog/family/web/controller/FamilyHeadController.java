package org.upyog.family.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.response.ResponseInfo;
import org.upyog.family.domain.service.FamilyHeadService;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.contract.AssignHofRequest;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * REST Controller exposing Head of Family role succession and transfer endpoints in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@RestController
@RequestMapping({"/v1/head", "/v1/hof", "/v1"})
@Slf4j
@Tag(name = "UPYOG Family Head Management", description = "Endpoints for transferring and assigning Head of Family (HoF) leadership roles")
public class FamilyHeadController {

    private final FamilyHeadService headService;
    private final ResponseInfoFactory responseInfoFactory;

    @Autowired
    public FamilyHeadController(FamilyHeadService headService, ResponseInfoFactory responseInfoFactory) {
        this.headService = headService;
        this.responseInfoFactory = responseInfoFactory;
    }

    /**
     * Transfers or assigns the Head of Family (HoF) role to an existing active member of the family unit in UPYOG.
     * Validates that the new head is an active member, revokes head status from the previous head, and updates family metadata.
     *
     * @param request the HoF assignment request envelope containing family ID, new head UUID, and optional justification
     * @return response entity containing the updated Family aggregate with HTTP 200 OK status
     */
    @PostMapping({"/_assign", "/_update", "/_assignHof"})
    @Operation(summary = "Transfer or assign Head of Family (HoF) role to an active member in UPYOG")
    public ResponseEntity<FamilyResponse> assignHeadOfFamily(@Valid @RequestBody AssignHofRequest request) {
        log.info("[UPYOG-HEAD-CONTROLLER] Received /_assignHof request for family: {} to new head: {}",
                request.getFamilyId(), request.getNewHeadUserUuid());
        Family updatedFamily = headService.assignHeadOfFamily(request);
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);

        FamilyResponse response = FamilyResponse.builder()
                .responseInfo(responseInfo)
                .families(Collections.singletonList(updatedFamily))
                .totalCount(1)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
