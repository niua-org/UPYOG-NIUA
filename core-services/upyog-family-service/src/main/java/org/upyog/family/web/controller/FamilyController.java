package org.upyog.family.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.upyog.family.domain.service.FamilyOtpService;
import org.upyog.family.domain.service.FamilyService;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMergeRequest;
import org.upyog.family.web.contract.FamilyOtpRequest;
import org.upyog.family.web.contract.FamilyOtpResponse;
import org.upyog.family.web.contract.FamilyOtpVerifyRequest;
import org.upyog.family.web.contract.FamilyOtpVerifyResponse;
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
 * <p>Handles HTTP transport, request validation, response serialization, and dispatches calls to
 * {@link FamilyService} and {@link FamilyOtpService}. All responses follow standard UPYOG contract envelopes
 * wrapping payload data alongside a standardized {@link ResponseInfo}.</p>
 *
 * <p><b>Endpoints Exposed under {@code /v1/family}:</b></p>
 * <ul>
 *   <li>{@code POST /_create} - Registers a new family unit with designated HoF and members.</li>
 *   <li>{@code POST /_search} - Queries families matching filters and enriches active member demographics.</li>
 *   <li>{@code POST /_update} - Mutates household attributes, members, and verification documents.</li>
 *   <li>{@code POST /_split} - Performs a genealogical split creating a child family.</li>
 *   <li>{@code POST /_merge} - Consolidates two families into a surviving unit.</li>
 *   <li>{@code POST /_sendOtp} - Dispatches an SMS verification OTP to the HoF's registered mobile number.</li>
 *   <li>{@code POST /_verifyOtp} - Validates the HoF OTP and returns family details with a verified consent token.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/family")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "UPYOG Family Registry", description = "Endpoints for registering, searching, updating, splitting and merging household units in UPYOG")
public class FamilyController {

    private final FamilyService familyService;
    private final FamilyOtpService familyOtpService;
    private final ResponseInfoFactory responseInfoFactory;

    /**
     * Registers a new Family Unit with an assigned Head of Family and initial members in UPYOG.
     *
     * <p>Generates a state-wide monotonic family identifier prefixed with {@code FAM-[cy:yyyy]-}
     * via {@code idgen}, establishes audit details, enqueues the record to Kafka persister,
     * and synchronously resolves citizen demographics.</p>
     *
     * @param request the family creation request envelope containing {@link RequestInfo} and the target {@link Family} entity;
     *                must not be {@code null}.
     * @return {@link ResponseEntity} wrapping a {@link FamilyResponse} containing the created {@link Family} aggregate
     *         with HTTP 201 CREATED status.
     * @throws org.egov.tracer.model.CustomException if request validation fails or business rules are violated.
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
     *
     * <p>Decorates matching households with active citizen profiles fetched in bulk from {@code egov-user}.
     * Supports search by citizen mobile number, which is resolved to the corresponding user UUID internally.</p>
     *
     * @param criteria query criteria containing search filters (tenantId, optional familyId, memberUserUuid, mobileNumber).
     * @param requestInfo UPYOG request context envelope containing auth token and caller identity; may be {@code null}.
     * @return {@link ResponseEntity} wrapping {@link FamilyResponse} containing the list of matching {@link Family} aggregates
     *         with HTTP 200 OK status.
     * @throws org.egov.tracer.model.CustomException if tenantId is missing or invalid.
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
     *
     * <p>Validates relationship constraints against MDMS master data, verifies exactly one active HoF,
     * updates system audit stamps, and enqueues update events to Kafka persister topic.</p>
     *
     * @param request the family update request envelope containing the modified {@link Family} attributes;
     *                must not be {@code null}.
     * @return {@link ResponseEntity} wrapping {@link FamilyResponse} containing the updated {@link Family} aggregate
     *         with HTTP 200 OK status.
     * @throws org.egov.tracer.model.CustomException if family does not exist, status is merged/inactive, or HoF count is invalid.
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
     *
     * <p>Deactivates the splitting members from the parent family, assigns the designated member as the new
     * child unit's HoF, issues a new sequential family ID, and inserts an audit entry into {@code ug_family_lineage}.</p>
     *
     * @param request the split request containing parent family ID, new head UUID, splitting member UUIDs, and justification;
     *                must not be {@code null}.
     * @return {@link ResponseEntity} wrapping {@link FamilyResponse} containing the newly created child {@link Family} aggregate
     *         with HTTP 201 CREATED status.
     * @throws org.egov.tracer.model.CustomException if parent family is not found, or splitting members are invalid.
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
     *
     * <p>Transfers active members from the merging family into the surviving family, marks the merging unit as
     * {@link org.upyog.family.domain.model.FamilyStatus#MERGED}, establishes the consolidated HoF, and logs lineage.</p>
     *
     * @param request the merge request containing surviving family ID, merging family ID, common head UUID, and merge reason;
     *                must not be {@code null}.
     * @return {@link ResponseEntity} wrapping {@link FamilyResponse} containing the consolidated surviving {@link Family} aggregate
     *         with HTTP 200 OK status.
     * @throws org.egov.tracer.model.CustomException if either family is missing or not active, or common head is invalid.
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

    /**
     * Initiates OTP dispatch to the Head of Family's registered mobile number for joining verification.
     *
     * <p>Used in the Family Join workflow when an applicant citizen inputs an existing {@code familyId}.
     * Resolves the current HoF, masks their mobile number (e.g. {@code ******3210}), generates an OTP
     * via {@code egov-otp}, and returns a {@code referenceId} alongside the masked phone number.</p>
     *
     * @param request the family OTP request envelope containing target {@code familyId} and {@code tenantId};
     *                must not be {@code null}.
     * @return {@link ResponseEntity} wrapping {@link FamilyOtpResponse} with masked mobile number and referenceId
     *         with HTTP 200 OK status.
     * @throws org.egov.tracer.model.CustomException if family is not found, HoF profile is missing, or OTP dispatch fails.
     */
    @PostMapping("/_sendOtp")
    @Operation(summary = "Send OTP to Head of Family mobile number for joining verification in UPYOG")
    public ResponseEntity<FamilyOtpResponse> sendFamilyOtp(@Valid @RequestBody FamilyOtpRequest request) {
        log.info("[UPYOG-FAMILY-CONTROLLER] Received /_sendOtp request for family: {}", request.getFamilyId());
        validateCallerUserInfo(request.getRequestInfo());
        FamilyOtpResponse response = familyOtpService.sendFamilyOtp(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Verifies the OTP sent to the Head of Family and returns enriched family details with a verified consent token.
     *
     * <p>Validates the OTP code against the HoF's mobile number. Upon successful verification, generates a secure 128-bit
     * consent token (prefixed with {@code CONSENT-VERIFIED-}) bound to the family and applicant, valid for completing the member join action.</p>
     *
     * @param request the family OTP verification request envelope containing {@code familyId}, {@code tenantId}, and {@code otp};
     *                must not be {@code null}.
     * @return {@link ResponseEntity} wrapping {@link FamilyOtpVerifyResponse} containing verified family details and consent token
     *         with HTTP 200 OK status.
     * @throws org.egov.tracer.model.CustomException if the OTP is invalid or expired (Code: {@code INVALID_OTP}), or family is not found.
     */
    @PostMapping("/_verifyOtp")
    @Operation(summary = "Verify OTP sent to Head of Family and retrieve family details in UPYOG")
    public ResponseEntity<FamilyOtpVerifyResponse> verifyFamilyOtp(@Valid @RequestBody FamilyOtpVerifyRequest request) {
        log.info("[UPYOG-FAMILY-CONTROLLER] Received /_verifyOtp request for family: {}", request.getFamilyId());
        validateCallerUserInfo(request.getRequestInfo());
        FamilyOtpVerifyResponse response = familyOtpService.verifyFamilyOtp(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Validates that caller user information is present and contains a valid user UUID in the request context.
     *
     * @param requestInfo UPYOG request context holding caller user details
     * @throws CustomException if requestInfo, userInfo, or user UUID is null or blank
     */
    private void validateCallerUserInfo(RequestInfo requestInfo) {
        if (requestInfo == null || requestInfo.getUserInfo() == null || StringUtils.isBlank(requestInfo.getUserInfo().getUuid())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Authenticated caller information (RequestInfo.userInfo.uuid) is required.");
        }
    }
}
