package org.upyog.family.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.response.ResponseInfo;
import org.upyog.family.domain.service.FamilyMemberService;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.contract.AddMemberRequest;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyResponse;
import org.upyog.family.web.contract.JoinFamilyRequest;
import org.upyog.family.web.contract.LeaveFamilyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * REST Controller exposing member lifecycle management endpoints (Add, Join, Leave) in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/member")
@Slf4j
@Tag(name = "UPYOG Family Member Management", description = "Endpoints for adding verified members with Aadhaar consent, joining families, and recording member exits")
public class FamilyMemberController {

    private final FamilyMemberService memberService;
    private final ResponseInfoFactory responseInfoFactory;

    @Autowired
    public FamilyMemberController(FamilyMemberService memberService, ResponseInfoFactory responseInfoFactory) {
        this.memberService = memberService;
        this.responseInfoFactory = responseInfoFactory;
    }

    /**
     * Adds a new verified member to an existing family unit with verified Aadhaar OTP consent in UPYOG.
     * Looks up or provisions the citizen record in the user service and links the member with ACTIVE status.
     *
     * @param request the add member request envelope containing member details, relationship with head, and consent token
     * @return response entity containing the updated Family aggregate with HTTP 200 OK status
     */
    @PostMapping("/_add")
    @Operation(summary = "Add a verified member to an existing Family Unit with Aadhaar OTP consent in UPYOG")
    public ResponseEntity<FamilyResponse> addMember(@Valid @RequestBody AddMemberRequest request) {
        log.info("[UPYOG-MEMBER-CONTROLLER] Received /_add request for family: {}", request.getFamilyId());
        Family updatedFamily = memberService.addMember(request);
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);

        FamilyResponse response = FamilyResponse.builder()
                .responseInfo(responseInfo)
                .families(Collections.singletonList(updatedFamily))
                .totalCount(1)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Processes an independent citizen request to join an existing family unit upon Head of Family authorization in UPYOG.
     * Verifies HoF authorization and registers the applicant as a member of the family.
     *
     * @param request the join family request envelope containing family ID, applicant UUID, and HoF consent token
     * @return response entity containing the updated Family aggregate with HTTP 200 OK status
     */
    @PostMapping("/_join")
    @Operation(summary = "Citizen requests to join an existing Family Unit with HoF authorization in UPYOG")
    public ResponseEntity<FamilyResponse> joinFamily(@Valid @RequestBody JoinFamilyRequest request) {
        log.info("[UPYOG-MEMBER-CONTROLLER] Received /_join request for family: {} by applicant: {}",
                request.getFamilyId(), request.getApplicantUserUuid());
        Family updatedFamily = memberService.joinFamily(request);
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);

        FamilyResponse response = FamilyResponse.builder()
                .responseInfo(responseInfo)
                .families(Collections.singletonList(updatedFamily))
                .totalCount(1)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Processes a member exit from a family unit due to marriage, demise, relocation, or other reasons in UPYOG.
     * Updates member status to EXITED, records exit date and reason, and prevents a solitary Head of Family from leaving.
     *
     * @param request the leave family request envelope containing family ID, member UUID, and exit reason
     * @return response entity containing the updated Family aggregate with HTTP 200 OK status
     */
    @PostMapping("/_leave")
    @Operation(summary = "Process a member leaving a Family Unit with exit reason and status tracking in UPYOG")
    public ResponseEntity<FamilyResponse> leaveFamily(@Valid @RequestBody LeaveFamilyRequest request) {
        log.info("[UPYOG-MEMBER-CONTROLLER] Received /_leaveFamily request for member: {} in family: {}",
                request.getMemberUserUuid(), request.getFamilyId());
        Family updatedFamily = memberService.leaveFamily(request);
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);

        FamilyResponse response = FamilyResponse.builder()
                .responseInfo(responseInfo)
                .families(Collections.singletonList(updatedFamily))
                .totalCount(1)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
