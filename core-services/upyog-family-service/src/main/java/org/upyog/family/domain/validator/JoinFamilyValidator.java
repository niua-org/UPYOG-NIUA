package org.upyog.family.domain.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.service.master.FamilyMasterDataService;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.RequesterDxClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.JoinFamilyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Specialized validator validating business rules for a citizen joining an existing Family Unit in UPYOG.
 *
 * <p>Validates applicant availability, dynamic MDMS relationship, and HoF approval consent.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class JoinFamilyValidator {

    private final FamilyRepository familyRepository;
    private final FamilyMasterDataService masterDataService;
    private final RequesterDxClient requesterDxClient;

    @Autowired
    public JoinFamilyValidator(FamilyRepository familyRepository,
                               FamilyMasterDataService masterDataService,
                               RequesterDxClient requesterDxClient) {
        this.familyRepository = familyRepository;
        this.masterDataService = masterDataService;
        this.requesterDxClient = requesterDxClient;
    }

    /**
     * Validates an independent citizen request to join an existing Family Unit in UPYOG.
     * Verifies Head of Family authorization consent, ensures applicant is not already active in another family,
     * and checks relationship against MDMS master data.
     *
     * @param request join family request containing applicant UUID and HoF consent token
     * @param existingFamily target Family aggregate
     * @throws CustomException if authorization or exclusivity constraints fail
     */
    public void validate(JoinFamilyRequest request, Family existingFamily) {
        log.info("[UPYOG-JOIN-FAMILY-VALIDATOR] Validating join family request: applicant={}, familyId={}",
                request.getApplicantUserUuid(), request.getFamilyId());

        if (existingFamily == null) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND,
                    "Family with ID " + request.getFamilyId() + " not found.");
        }

        if (existingFamily.getStatus() != FamilyStatus.ACTIVE) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Cannot join a family with status: " + existingFamily.getStatus());
        }

        if (StringUtils.isBlank(request.getApplicantUserUuid())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Applicant userUuid is mandatory.");
        }

        // Validate relationship against UPYOG MDMS
        masterDataService.validateRelationship(request.getRequestInfo(), request.getTenantId(), request.getRelationshipWithHead());

        // Check if applicant already belongs to an active family
        List<String> activeFamilyIds = familyRepository.findActiveFamilyIdsByMemberUuid(request.getApplicantUserUuid());
        if (!activeFamilyIds.isEmpty()) {
            throw new CustomException(FamilyConstants.ERROR_MEMBER_ALREADY_EXISTS,
                    "Applicant already belongs to active family: " + activeFamilyIds.get(0));
        }

        // Validate HoF consent / OTP authorization
        if (StringUtils.isBlank(request.getHofConsentOtp())) {
            throw new CustomException(FamilyConstants.ERROR_CONSENT_INVALID,
                    "Head of Family authorization OTP / consent token is mandatory to join this family.");
        }

        boolean consentValid = requesterDxClient.verifyConsentToken(request.getHofConsentOtp(), request.getTenantId(), request.getRequestInfo());
        if (!consentValid) {
            throw new CustomException(FamilyConstants.ERROR_CONSENT_INVALID,
                    "Head of Family consent verification failed.");
        }

        log.info("[UPYOG-JOIN-FAMILY-VALIDATOR] Successfully validated join family request for applicant: {}", request.getApplicantUserUuid());
    }
}
