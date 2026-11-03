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

    @Override
    public FamilyOtpResponse sendFamilyOtp(FamilyOtpRequest request) {
        log.info("[UPYOG-FAMILY-OTP-SERVICE] Processing send OTP request for family: {}, tenant: {}",
                request.getFamilyId(), request.getTenantId());

        FamilySearchCriteria criteria = FamilySearchCriteria.builder()
                .familyId(request.getFamilyId())
                .tenantId(request.getTenantId())
                .build();

        List<Family> families = familyRepository.search(criteria);
        if (families.isEmpty()) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND,
                    "Family with ID " + request.getFamilyId() + " not found.");
        }

        Family family = families.get(0);
        if (family.getStatus() != FamilyStatus.ACTIVE) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Cannot send OTP for inactive or closed family: " + family.getStatus());
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
        String referenceId = otpClient != null
                ? otpClient.sendOtp(mobileNumber, request.getTenantId(), request.getRequestInfo())
                : UUID.randomUUID().toString();
        String maskedMobile = maskMobileNumber(mobileNumber);

        ResponseInfo responseInfo = responseInfoFactory != null
                ? responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true)
                : null;

        return FamilyOtpResponse.builder()
                .responseInfo(responseInfo)
                .familyId(family.getFamilyId())
                .maskedMobileNumber(maskedMobile)
                .referenceId(referenceId)
                .isSent(true)
                .build();
    }

    @Override
    public FamilyOtpVerifyResponse verifyFamilyOtp(FamilyOtpVerifyRequest request) {
        log.info("[UPYOG-FAMILY-OTP-SERVICE] Processing verify OTP request for family: {}, tenant: {}",
                request.getFamilyId(), request.getTenantId());

        FamilySearchCriteria criteria = FamilySearchCriteria.builder()
                .familyId(request.getFamilyId())
                .tenantId(request.getTenantId())
                .build();

        List<Family> families = familyRepository.search(criteria);
        if (families.isEmpty()) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND,
                    "Family with ID " + request.getFamilyId() + " not found.");
        }

        Family family = families.get(0);
        if (family.getStatus() != FamilyStatus.ACTIVE) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Cannot verify OTP for inactive family: " + family.getStatus());
        }

        String headUserUuid = family.getHeadUserUuid();
        Map<String, CitizenProfile> profileMap = userClient.searchUsersByUuids(
                Collections.singletonList(headUserUuid), request.getRequestInfo());
        CitizenProfile headProfile = profileMap.get(headUserUuid);
        if (headProfile == null || StringUtils.isBlank(headProfile.getMobileNumber())) {
            throw new CustomException(FamilyConstants.ERROR_USER_NOT_FOUND,
                    "Mobile number for Head of Family not found.");
        }

        boolean isValid = otpClient != null && otpClient.validateOtp(
                headProfile.getMobileNumber(), request.getOtp(), request.getTenantId(), request.getRequestInfo());
        if (!isValid) {
            throw new CustomException(FamilyConstants.ERROR_CONSENT_INVALID,
                    "Invalid or expired OTP entered.");
        }

        // Enrich family with citizen demographics
        enrichCitizenDemographics(Collections.singletonList(family), request.getRequestInfo());

        // Generate verified HoF consent token
        String consentToken = "CONSENT-VERIFIED-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        if (requesterDxClient != null) {
            requesterDxClient.registerConsentToken(consentToken, family.getFamilyId());
        }

        ResponseInfo responseInfo = responseInfoFactory != null
                ? responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true)
                : null;

        return FamilyOtpVerifyResponse.builder()
                .responseInfo(responseInfo)
                .family(family)
                .consentToken(consentToken)
                .isVerified(true)
                .build();
    }

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
                        member.setCitizenDetails(profileMap.get(member.getUserUuid()));
                    }
                }
            }
        }
    }

    private String maskMobileNumber(String mobileNumber) {
        if (StringUtils.isBlank(mobileNumber)) {
            return "******";
        }
        String trimmed = mobileNumber.trim();
        if (trimmed.length() <= 4) {
            return "******" + trimmed;
        }
        return "******" + trimmed.substring(trimmed.length() - 4);
    }
}
