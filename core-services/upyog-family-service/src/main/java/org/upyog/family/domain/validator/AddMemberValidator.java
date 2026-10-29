package org.upyog.family.domain.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.service.master.FamilyMasterDataService;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.RequesterDxClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.AddMemberRequest;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Specialized validator validating business rules for adding a member to an existing Family Unit in UPYOG.
 *
 * <p>Enforces dynamic MDMS relationship verification, active family exclusivity, and Aadhaar OTP consent.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class AddMemberValidator {

    private final FamilyRepository familyRepository;
    private final FamilyMasterDataService masterDataService;
    private final RequesterDxClient requesterDxClient;

    @Autowired
    public AddMemberValidator(FamilyRepository familyRepository,
                              FamilyMasterDataService masterDataService,
                              RequesterDxClient requesterDxClient) {
        this.familyRepository = familyRepository;
        this.masterDataService = masterDataService;
        this.requesterDxClient = requesterDxClient;
    }

    /**
     * Validates adding a member to an existing Family Unit in UPYOG.
     * Verifies Aadhaar OTP consent via requester-services-dx, checks member exclusivity,
     * and validates relationship against MDMS master data.
     *
     * @param request add member request containing member attributes and consent token
     * @param existingFamily target Family aggregate
     * @throws CustomException if consent is invalid or member is already in an active family
     */
    public void validate(AddMemberRequest request, Family existingFamily) {
        log.info("[UPYOG-ADD-MEMBER-VALIDATOR] Validating add member request for familyId: {}", request.getFamilyId());

        if (existingFamily == null) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND,
                    "Family with ID " + request.getFamilyId() + " not found.");
        }

        if (existingFamily.getStatus() != FamilyStatus.ACTIVE) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Cannot add members to a family with status: " + existingFamily.getStatus());
        }

        // Caller authorization: Only current HoF is authorized to add members
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null) {
            String callerUuid = request.getRequestInfo().getUserInfo().getUuid();
            if (StringUtils.isNotBlank(callerUuid) && !FamilyConstants.SYSTEM_USER.equalsIgnoreCase(callerUuid)) {
                if (!callerUuid.equals(existingFamily.getHeadUserUuid())) {
                    throw new CustomException(FamilyConstants.ERROR_UNAUTHORIZED_ACCESS,
                            "Only the Head of Family is authorized to add new members to the family unit.");
                }
            }
        }

        FamilyMember member = request.getMember();
        if (member == null || (StringUtils.isBlank(member.getUserUuid()) && StringUtils.isBlank(member.getMobileNumber()))) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Either member userUuid or mobileNumber is mandatory for adding a family member.");
        }

        // 1. Validate relationship against UPYOG MDMS
        if (StringUtils.isBlank(member.getRelationshipWithHead())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_RELATIONSHIP,
                    "Relationship with Head of Family is mandatory.");
        }
        masterDataService.validateRelationship(request.getRequestInfo(), request.getTenantId(), member.getRelationshipWithHead());

        // 2. Cannot add as Head of Family via addMember (must use assignHof)
        if (Boolean.TRUE.equals(member.getIsHead())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_HOF_ASSIGNMENT,
                    "A new member cannot be declared Head of Family during addition. Use HOF transfer API.");
        }

        // 3. Ensure member is not already an active member of this family
        if (existingFamily.getMembers() != null && StringUtils.isNotBlank(member.getUserUuid())) {
            boolean alreadyActive = existingFamily.getMembers().stream()
                    .anyMatch(m -> member.getUserUuid().equals(m.getUserUuid()) && Boolean.TRUE.equals(m.getActive()));
            if (alreadyActive) {
                throw new CustomException(FamilyConstants.ERROR_MEMBER_ALREADY_EXISTS,
                        "Citizen is already an active member of family " + existingFamily.getFamilyId());
            }
        }

        // 4. Ensure member is not active in any OTHER family unit (One-Citizen-One-Active-Family Rule)
        if (StringUtils.isNotBlank(member.getUserUuid())) {
            List<String> activeFamilyIds = familyRepository.findActiveFamilyIdsByMemberUuid(member.getUserUuid());
            for (String activeFamId : activeFamilyIds) {
                if (!activeFamId.equals(existingFamily.getFamilyId())) {
                    throw new CustomException(FamilyConstants.ERROR_MEMBER_ALREADY_EXISTS,
                            "Citizen " + member.getUserUuid() + " already belongs to active family: " + activeFamId);
                }
            }
        }

        // 5. Aadhaar OTP Consent Verification (from requester-services-dx)
        String consentToken = StringUtils.isNotBlank(request.getConsentToken())
                ? request.getConsentToken()
                : member.getConsentToken();

        boolean isMinor = (member.getAdditionalDetails() != null && member.getAdditionalDetails().toString().contains("isMinor"))
                || "CHILD".equalsIgnoreCase(member.getRelationshipWithHead())
                || "DEPENDENT".equalsIgnoreCase(member.getRelationshipWithHead());

        if (StringUtils.isBlank(consentToken)) {
            if (!isMinor) {
                throw new CustomException(FamilyConstants.ERROR_CONSENT_INVALID,
                        "Aadhaar OTP verification consent token is required to add an adult member to the family.");
            }
            log.info("[UPYOG-ADD-MEMBER-VALIDATOR] Member is a minor/dependent under Head of Family guardian. OTP consent bypassed for minor.");
        } else {
            String memberIdentifier = StringUtils.isNotBlank(member.getUserUuid())
                    ? member.getUserUuid()
                    : member.getMobileNumber();

            boolean consentVerified = requesterDxClient.verifyConsentToken(
                    consentToken,
                    memberIdentifier,
                    existingFamily.getFamilyId(),
                    request.getTenantId(),
                    request.getRequestInfo());
            if (!consentVerified) {
                throw new CustomException(FamilyConstants.ERROR_CONSENT_INVALID,
                        "Aadhaar OTP verification failed. Member consent could not be validated.");
            }
        }

        log.info("[UPYOG-ADD-MEMBER-VALIDATOR] Successfully validated add member request for citizen: {}", member.getUserUuid());
    }
}
