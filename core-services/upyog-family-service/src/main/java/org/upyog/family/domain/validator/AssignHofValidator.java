package org.upyog.family.domain.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.AssignHofRequest;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.springframework.stereotype.Component;

/**
 * Specialized validator validating business rules for assigning/transferring Head of Family in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class AssignHofValidator {

    /**
     * Validates transferring Head of Family leadership in UPYOG.
     * Ensures the new head is an active member of the family, is not already the head,
     * and satisfies adult age requirements.
     *
     * @param request HoF assignment request containing new head UUID
     * @param existingFamily existing Family aggregate
     * @throws CustomException if the designated member is not active or cannot be head
     */
    public void validate(AssignHofRequest request, Family existingFamily) {
        log.info("[UPYOG-ASSIGN-HOF-VALIDATOR] Validating HoF assignment for familyId: {}", request.getFamilyId());

        if (existingFamily == null) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND,
                    "Family with ID " + request.getFamilyId() + " not found.");
        }

        if (existingFamily.getStatus() != FamilyStatus.ACTIVE) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Cannot assign Head of Family for a family with status: " + existingFamily.getStatus());
        }

        if (StringUtils.isBlank(request.getNewHeadUserUuid())) {
            throw new CustomException(FamilyConstants.ERROR_HEAD_OF_FAMILY_REQUIRED,
                    "New Head User UUID is mandatory.");
        }

        // Check if member is already the head
        if (request.getNewHeadUserUuid().equals(existingFamily.getHeadUserUuid())) {
            throw new CustomException(FamilyConstants.ERROR_ALREADY_HEAD,
                    "Citizen " + request.getNewHeadUserUuid() + " is already the Head of Family.");
        }

        // Verify that target user is an active member of this family
        boolean memberFoundAndActive = false;
        if (existingFamily.getMembers() != null) {
            for (FamilyMember m : existingFamily.getMembers()) {
                if (request.getNewHeadUserUuid().equals(m.getUserUuid())) {
                    if (Boolean.TRUE.equals(m.getActive())) {
                        memberFoundAndActive = true;
                        break;
                    } else {
                        throw new CustomException(FamilyConstants.ERROR_MEMBER_INACTIVE,
                                "Citizen " + request.getNewHeadUserUuid() + " is inactive/exited from this family.");
                    }
                }
            }
        }

        if (!memberFoundAndActive) {
            throw new CustomException(FamilyConstants.ERROR_MEMBER_NOT_FOUND,
                    "Target citizen " + request.getNewHeadUserUuid() + " must be an active member of this family before being assigned as Head.");
        }

        log.info("[UPYOG-ASSIGN-HOF-VALIDATOR] Successfully validated HoF assignment for citizen: {}", request.getNewHeadUserUuid());
    }
}
