package org.upyog.family.domain.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.LeaveFamilyRequest;
import org.springframework.stereotype.Component;

/**
 * Specialized validator validating business rules for a member leaving an existing Family Unit in UPYOG.
 *
 * <p>Ensures that the Head of Family cannot leave without designating a successor HoF unless the family unit is dissolving.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class LeaveFamilyValidator {

    /**
     * Validates a member departure from a Family Unit in UPYOG.
     * Ensures target member exists and is active, validates exit reason,
     * and prevents a sole Head of Family from leaving without first transferring leadership.
     *
     * @param request leave family request containing member UUID and exit reason
     * @param existingFamily existing Family aggregate
     * @throws CustomException if member is not found or is the solitary HoF
     */
    public void validate(LeaveFamilyRequest request, Family existingFamily) {
        log.info("[UPYOG-LEAVE-FAMILY-VALIDATOR] Validating leave request: member={}, familyId={}",
                request.getMemberUserUuid(), request.getFamilyId());

        if (existingFamily == null) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND,
                    "Family with ID " + request.getFamilyId() + " not found.");
        }

        if (StringUtils.isBlank(request.getMemberUserUuid())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Member userUuid is mandatory for leaving family.");
        }

        if (request.getExitReason() == null) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Exit reason (MARRIAGE, DEMISE, RELOCATION, OTHER) is mandatory.");
        }

        // Caller authorization: Only the member themselves or the current HoF can process member departure
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null) {
            String callerUuid = request.getRequestInfo().getUserInfo().getUuid();
            if (StringUtils.isNotBlank(callerUuid) && !FamilyConstants.SYSTEM_USER.equalsIgnoreCase(callerUuid)) {
                boolean isSelf = callerUuid.equals(request.getMemberUserUuid());
                boolean isHof = callerUuid.equals(existingFamily.getHeadUserUuid());
                if (!isSelf && !isHof) {
                    throw new CustomException(FamilyConstants.ERROR_UNAUTHORIZED_ACCESS,
                            "Only the member themselves or the Head of Family is authorized to leave/exit the family unit.");
                }
            }
        }

        // Find the member in the family
        FamilyMember memberToLeave = null;
        int activeMembersCount = 0;

        if (existingFamily.getMembers() != null) {
            for (FamilyMember m : existingFamily.getMembers()) {
                if (Boolean.TRUE.equals(m.getActive())) {
                    activeMembersCount++;
                    if (request.getMemberUserUuid().equals(m.getUserUuid())) {
                        memberToLeave = m;
                    }
                }
            }
        }

        if (memberToLeave == null) {
            throw new CustomException(FamilyConstants.ERROR_MEMBER_NOT_FOUND,
                    "Member " + request.getMemberUserUuid() + " is not an active member of family " + existingFamily.getFamilyId());
        }

        // If the member leaving is the Head of Family, ensure they cannot leave if other active members remain
        if (Boolean.TRUE.equals(memberToLeave.getIsHead()) || request.getMemberUserUuid().equals(existingFamily.getHeadUserUuid())) {
            if (activeMembersCount > 1) {
                throw new CustomException(FamilyConstants.ERROR_HEAD_CANNOT_LEAVE,
                        "Head of Family cannot leave the family without first transferring the HoF role to another active member.");
            }
        }

        log.info("[UPYOG-LEAVE-FAMILY-VALIDATOR] Successfully validated leave request for member: {}", request.getMemberUserUuid());
    }
}
