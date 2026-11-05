package org.upyog.family.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Service;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.OtpClient;
import org.upyog.family.persistence.repository.RequesterDxClient;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.util.MaskUtils;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilyOtpRequest;
import org.upyog.family.web.contract.FamilyOtpResponse;
import org.upyog.family.web.contract.FamilyOtpVerifyRequest;
import org.upyog.family.web.contract.FamilyOtpVerifyResponse;
import org.upyog.family.web.contract.FamilySearchCriteria;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Domain service implementation orchestrating Head of Family OTP authorization and consent token issuance.
 *
 * <p>Strictly adheres to Single Responsibility Principle (SRP) by decoupling OTP authorization
 * workflow from core Family Unit lifecycle management.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyOtpServiceImpl implements FamilyOtpService {

    private final FamilyRepository familyRepository;
    private final UserClient userClient;
    private final OtpClient otpClient;
    private final RequesterDxClient requesterDxClient;
    private final ResponseInfoFactory responseInfoFactory;

    /**
     * Initiates SMS OTP dispatch to the Head of Family's registered mobile number for join verification.
     *
     * <p>Finds the family unit by ID and tenant, looks up the active HoF's profile from {@code egov-user},
     * masks the mobile number (e.g. {@code ******3210}), issues an OTP via {@link OtpClient#sendOtp},
     * and returns the tracking reference ID.</p>
     *
     * @param request the family OTP request containing family ID and tenant ID; must not be {@code null}.
     * @return {@link FamilyOtpResponse} containing masked mobile, reference ID, and dispatch status.
     * @throws CustomException if family is not found, status is inactive, or HoF mobile cannot be resolved.
     */
    @Override
    public FamilyOtpResponse sendFamilyOtp(FamilyOtpRequest request) {
        log.info("[UPYOG-FAMILY-OTP-SERVICE] Processing send OTP request for family: {}, tenant: {}",
                request.getFamilyId(), request.getTenantId());

        // Ensure caller is identity-verified in user registry database
        userClient.validateCallerIsVerified(request.getRequestInfo());

        String callerUuid = request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null
                ? request.getRequestInfo().getUserInfo().getUuid()
                : null;
        if (StringUtils.isBlank(callerUuid)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Authenticated caller information is required to send family OTP.");
        }

        Family family = familyRepository.findByFamilyIdAndTenantId(request.getFamilyId(), request.getTenantId())
                .orElseThrow(() -> new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND,
                        "Family with ID " + request.getFamilyId() + " not found."));

        if (family.getStatus() != FamilyStatus.ACTIVE) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Cannot send OTP for inactive or closed family: " + family.getStatus());
        }

        if (family.getMembers() != null) {
            boolean alreadyMember = family.getMembers().stream()
                    .anyMatch(m -> m.getUserUuid() != null && m.getUserUuid().equals(callerUuid));
            if (alreadyMember) {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                        "Caller " + callerUuid + " is already an active member of family " + family.getFamilyId());
            }
        }

        String headUserUuid = family.getHeadUserUuid();
        if (StringUtils.isBlank(headUserUuid)) {
            throw new CustomException(FamilyConstants.ERROR_USER_NOT_FOUND,
                    "Head of family record not associated with family: " + request.getFamilyId());
        }

        Map<String, CitizenProfile> profileMap = userClient.searchUsersByUuids(
                Collections.singletonList(headUserUuid), request.getRequestInfo());
        CitizenProfile headProfile = profileMap.get(headUserUuid);
        if (headProfile == null || StringUtils.isBlank(headProfile.getMobileNumber())) {
            throw new CustomException(FamilyConstants.ERROR_USER_NOT_FOUND,
                    "Mobile number for Head of Family " + headUserUuid + " not found.");
        }

        String mobileNumber = headProfile.getMobileNumber();
        String referenceId = otpClient.sendOtp(mobileNumber, request.getTenantId(), request.getRequestInfo());
        String maskedMobile = MaskUtils.maskMobileLast4(mobileNumber);

        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);

        return FamilyOtpResponse.builder()
                .responseInfo(responseInfo)
                .familyId(family.getFamilyId())
                .maskedMobileNumber(maskedMobile)
                .referenceId(referenceId)
                .isSent(true)
                .build();
    }

    /**
     * Verifies the OTP entered by citizen against Head of Family's phone and returns family details with consent token.
     *
     * <p>Validates the OTP code via {@link OtpClient#validateOtp}, decorates member demographics via
     * {@link #enrichCitizenDemographics}, generates a unique 128-bit consent token bound to the family and applicant,
     * registers the consent with {@link RequesterDxClient#registerConsentToken}, and returns the verified aggregate.</p>
     *
     * @param request the verification request envelope containing family ID, tenant ID, and OTP code; must not be {@code null}.
     * @return {@link FamilyOtpVerifyResponse} containing verified family aggregate, consent token, and verification state.
     * @throws CustomException if family is not found, status is inactive, or the entered OTP is invalid/expired.
     */
    @Override
    public FamilyOtpVerifyResponse verifyFamilyOtp(FamilyOtpVerifyRequest request) {
        log.info("[UPYOG-FAMILY-OTP-SERVICE] Processing verify OTP request for family: {}, tenant: {}",
                request.getFamilyId(), request.getTenantId());

        // Ensure caller is identity-verified in user registry database
        userClient.validateCallerIsVerified(request.getRequestInfo());

        String callerUuid = request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null
                ? request.getRequestInfo().getUserInfo().getUuid()
                : null;
        if (StringUtils.isBlank(callerUuid)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Authenticated caller information is required to verify family OTP.");
        }

        if (StringUtils.isBlank(request.getReferenceId())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "referenceId is mandatory for OTP verification.");
        }

        Family family = familyRepository.findByFamilyIdAndTenantId(request.getFamilyId(), request.getTenantId())
                .orElseThrow(() -> new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND,
                        "Family with ID " + request.getFamilyId() + " not found."));
        if (family.getStatus() != FamilyStatus.ACTIVE) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Cannot verify OTP for inactive family: " + family.getStatus());
        }

        if (family.getMembers() != null) {
            boolean alreadyMember = family.getMembers().stream()
                    .anyMatch(m -> m.getUserUuid() != null && m.getUserUuid().equals(callerUuid));
            if (alreadyMember) {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                        "Caller " + callerUuid + " is already an active member of family " + family.getFamilyId());
            }
        }

        String headUserUuid = family.getHeadUserUuid();
        Map<String, CitizenProfile> profileMap = userClient.searchUsersByUuids(
                Collections.singletonList(headUserUuid), request.getRequestInfo());
        CitizenProfile headProfile = profileMap.get(headUserUuid);
        if (headProfile == null || StringUtils.isBlank(headProfile.getMobileNumber())) {
            throw new CustomException(FamilyConstants.ERROR_USER_NOT_FOUND,
                    "Mobile number for Head of Family not found.");
        }

        boolean isValid = otpClient.validateOtp(
                headProfile.getMobileNumber(), request.getOtp(), request.getTenantId(), request.getRequestInfo());
        if (!isValid) {
            throw new CustomException(FamilyConstants.ERROR_CONSENT_INVALID,
                    "Invalid or expired OTP entered.");
        }

        // Enrich family with citizen demographics
        enrichCitizenDemographics(Collections.singletonList(family), request.getRequestInfo());

        // Generate verified 128-bit HoF consent token bound to target family and applicant
        String consentToken = "CONSENT-VERIFIED-" + UUID.randomUUID().toString().replace("-", "");
        requesterDxClient.registerConsentToken(consentToken, family.getFamilyId(), callerUuid);

        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);

        // Sanitize family demographics to minimize sensitive PII returned to join applicant
        Family sanitizedFamily = sanitizeFamilyForJoin(family);

        return FamilyOtpVerifyResponse.builder()
                .responseInfo(responseInfo)
                .family(sanitizedFamily)
                .consentToken(consentToken)
                .isVerified(true)
                .build();
    }

    /**
     * Sanitizes family details and masks member mobile numbers to protect PII when displaying to an applicant.
     *
     * @param original original Family domain aggregate
     * @return sanitized Family instance with masked mobile numbers
     */
    private Family sanitizeFamilyForJoin(Family original) {
        if (original == null) {
            return null;
        }
        List<FamilyMember> sanitizedMembers = null;
        if (original.getMembers() != null) {
            sanitizedMembers = original.getMembers().stream().map(m -> {
                FamilyMember copy = new FamilyMember();
                copy.setId(m.getId());
                copy.setFamilyId(m.getFamilyId());
                copy.setTenantId(m.getTenantId());
                copy.setUserUuid(m.getUserUuid());
                copy.setName(m.getName());
                copy.setRelationshipWithHead(m.getRelationshipWithHead());
                copy.setStatus(m.getStatus());
                copy.setIsHead(m.getIsHead());
                copy.setActive(m.getActive());
                copy.setMobileNumber(MaskUtils.maskMobile(m.getMobileNumber()));
                return copy;
            }).collect(Collectors.toList());
        }

        return Family.builder()
                .id(original.getId())
                .familyId(original.getFamilyId())
                .tenantId(original.getTenantId())
                .status(original.getStatus())
                .headUserUuid(original.getHeadUserUuid())
                .category(original.getCategory())
                .members(sanitizedMembers)
                .build();
    }

    /**
     * Enriches family members with live citizen demographic data retrieved from egov-user service.
     *
     * @param families list of Family domain aggregates to enrich
     * @param requestInfo UPYOG request context envelope
     */
    private void enrichCitizenDemographics(List<Family> families, RequestInfo requestInfo) {
        if (families == null || families.isEmpty()) {
            return;
        }

        List<String> allMemberUuids = families.stream()
                .filter(f -> f.getMembers() != null)
                .flatMap(f -> f.getMembers().stream())
                .map(FamilyMember::getUserUuid)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        if (allMemberUuids.isEmpty()) {
            return;
        }

        Map<String, CitizenProfile> profileMap = userClient.searchUsersByUuids(allMemberUuids, requestInfo);
        for (Family family : families) {
            if (family.getMembers() != null) {
                for (FamilyMember member : family.getMembers()) {
                    if (profileMap.containsKey(member.getUserUuid())) {
                        member.enrichFromCitizenProfile(profileMap.get(member.getUserUuid()));
                    }
                }
                family.enrichHeadName();
            }
        }
    }
}
