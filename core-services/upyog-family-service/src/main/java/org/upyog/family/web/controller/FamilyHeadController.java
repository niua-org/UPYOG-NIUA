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
import org.upyog.family.web.contract.FamilyHofCheckRequest;
import org.upyog.family.web.contract.FamilyHofCheckResponse;
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
 * <p>Handles HTTP transport and serialization for household representation succession flows. Coordinates
 * with {@link FamilyHeadService} to ensure that only authorized heads can transfer representation and that
 * the target successor is an active resident member of the family unit.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/head")
@Slf4j
@Tag(name = "UPYOG Family Head Management", description = "Endpoints for transferring and assigning Head of Family (HoF) leadership roles")
public class FamilyHeadController {

    private final FamilyHeadService headService;
    private final ResponseInfoFactory responseInfoFactory;

    /**
     * Constructs {@link FamilyHeadController} with required domain service and response factory.
     *
     * @param headService service orchestrating HoF succession
     * @param responseInfoFactory utility creating standard UPYOG {@link ResponseInfo} headers
     */
    @Autowired
    public FamilyHeadController(FamilyHeadService headService, ResponseInfoFactory responseInfoFactory) {
        this.headService = headService;
        this.responseInfoFactory = responseInfoFactory;
    }

    /**
     * Transfers or assigns the Head of Family (HoF) role to an existing active member of the family unit in UPYOG.
     *
     * <p>Enforces caller authorization (current HoF only), verifies candidate member status, revokes headship
     * from the incumbent, promotes the successor, and records audit trail before publishing to Kafka.</p>
     *
     * @param request the HoF assignment request envelope containing family ID, new head UUID, and optional justification;
     *                must not be {@code null}.
     * @return {@link ResponseEntity} wrapping {@link FamilyResponse} containing the updated {@link Family} aggregate
     *         with HTTP 200 OK status.
     * @throws org.egov.tracer.model.CustomException if caller is unauthorized, target family or member is not found,
     *         or candidate is already the HoF.
     */
    @PostMapping("/_assign")
    @Operation(summary = "Transfer or assign Head of Family (HoF) role to an active member in UPYOG")
    public ResponseEntity<FamilyResponse> assignHeadOfFamily(@Valid @RequestBody AssignHofRequest request) {
        log.info("[UPYOG-HEAD-CONTROLLER] Received /_assign request for family: {} to new head: {}",
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

    /**
     * Checks whether the authenticated caller in {@code RequestInfo} is the Head of Family (HoF).
     *
     * @param request the HoF check request envelope containing {@code RequestInfo}, {@code familyId}, and optional {@code tenantId};
     *                must not be {@code null}.
     * @return {@link ResponseEntity} wrapping {@link FamilyHofCheckResponse} stating whether the user is HoF,
     *         with HTTP 200 OK status.
     * @throws org.egov.tracer.model.CustomException if caller is unauthenticated or target family is not found.
     */
    @PostMapping("/_isHof")
    @Operation(summary = "Check whether the authenticated caller is the Head of Family (HoF)")
    public ResponseEntity<FamilyHofCheckResponse> isHeadOfFamily(@Valid @RequestBody FamilyHofCheckRequest request) {
        log.info("[UPYOG-HEAD-CONTROLLER] Received /_isHof request for family: {}", request.getFamilyId());
        FamilyHofCheckResponse response = headService.isHeadOfFamily(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
