package org.upyog.family.domain.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.service.master.FamilyMasterDataService;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.FileStoreClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyDocument;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilyMergeRequest;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.FamilySplitRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Domain validator enforcing business rules, unique Head of Family, multi-family exclusivity,
 * and dynamic UPYOG MDMS relationship validation.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class FamilyValidator {

    private final FamilyRepository familyRepository;
    private final FileStoreClient fileStoreClient;
    private final FamilyMasterDataService familyMasterDataService;

    @Autowired
    public FamilyValidator(FamilyRepository familyRepository,
                           FileStoreClient fileStoreClient,
                           FamilyMasterDataService familyMasterDataService) {
        this.familyRepository = familyRepository;
        this.fileStoreClient = fileStoreClient;
        this.familyMasterDataService = familyMasterDataService;
    }

    /**
     * Validates a family creation request against UPYOG business rules.
     * Enforces single active Head of Family, member UUID uniqueness, MDMS relationship codes,
     * document proofs in filestore, and multi-family exclusivity.
     *
     * @param request family creation request containing family metadata and members
     * @throws CustomException if validation constraints fail
     */
    public void validateCreate(FamilyRequest request) {
        if (request == null || request.getFamily() == null) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "Family request body cannot be null");
        }

        Family family = request.getFamily();

        if (StringUtils.isBlank(family.getTenantId())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_FAMILY_TENANT_REQUIRED);
        }

        if (family.getMembers() == null || family.getMembers().isEmpty()) {
            throw new CustomException(FamilyConstants.ERROR_HEAD_OF_FAMILY_REQUIRED, FamilyConstants.MSG_HEAD_OF_FAMILY_MANDATORY);
        }

        // 1. Verify single Head of Family (HoF)
        long headCount = family.getMembers().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsHead()))
                .count();

        if (headCount != 1) {
            log.error("[UPYOG-FAMILY-VALIDATOR] Invalid HoF count: {} for tenant: {}", headCount, family.getTenantId());
            throw new CustomException(FamilyConstants.ERROR_HEAD_OF_FAMILY_REQUIRED, FamilyConstants.MSG_HEAD_OF_FAMILY_MANDATORY);
        }

        FamilyMember headMember = family.getMembers().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsHead()))
                .findFirst()
                .orElseThrow(() -> new CustomException(FamilyConstants.ERROR_HEAD_OF_FAMILY_REQUIRED, FamilyConstants.MSG_HEAD_OF_FAMILY_MANDATORY));

        if (StringUtils.isNotBlank(family.getHeadUserUuid()) && !family.getHeadUserUuid().equals(headMember.getUserUuid())) {
            log.error("[UPYOG-FAMILY-VALIDATOR] Mismatch between family.headUserUuid ({}) and head member userUuid ({})",
                    family.getHeadUserUuid(), headMember.getUserUuid());
            throw new CustomException(FamilyConstants.ERROR_HEAD_UUID_MISMATCH, "family.headUserUuid must match the userUuid of the member where isHead is true");
        }

        // 2. Validate member UUID uniqueness, MDMS relationship, and multi-family exclusivity
        Set<String> memberUuids = new HashSet<>();
        for (FamilyMember member : family.getMembers()) {
            if (StringUtils.isBlank(member.getUserUuid())) {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "Every family member must have a valid userUuid");
            }
            if (!memberUuids.add(member.getUserUuid())) {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "Duplicate member userUuid found in request: " + member.getUserUuid());
            }

            // Dynamic MDMS validation for relationshipWithHead
            if (StringUtils.isNotBlank(member.getRelationshipWithHead())) {
                familyMasterDataService.validateRelationship(request.getRequestInfo(), family.getTenantId(), member.getRelationshipWithHead());
            }

            // Exclusivity rule: Member cannot belong to another active family
            List<String> activeFamilyIds = familyRepository.findActiveFamilyIdsByMemberUuid(member.getUserUuid());
            if (activeFamilyIds != null && !activeFamilyIds.isEmpty()) {
                log.error("[UPYOG-FAMILY-VALIDATOR] Citizen {} already exists in active family: {}", member.getUserUuid(), activeFamilyIds);
                throw new CustomException(FamilyConstants.ERROR_MEMBER_ALREADY_EXISTS,
                        "Citizen profile " + member.getUserUuid() + " is already an active member of family: " + activeFamilyIds.get(0));
            }
        }

        // 3. Validate documents
        if (family.getDocuments() != null) {
            for (FamilyDocument doc : family.getDocuments()) {
                if (StringUtils.isBlank(doc.getFilestoreId())) {
                    throw new CustomException(FamilyConstants.ERROR_FILESTORE_NOT_FOUND, "Document filestoreId is mandatory");
                }
                boolean exists = fileStoreClient.validateFileExists(family.getTenantId(), doc.getFilestoreId());
                if (!exists) {
                    throw new CustomException(FamilyConstants.ERROR_FILESTORE_NOT_FOUND,
                            "FileStore ID does not exist or is inaccessible: " + doc.getFilestoreId());
                }
            }
        }
    }

    /**
     * Validates an update request against an existing active Family aggregate in UPYOG.
     * Verifies immutable family ID, ensures at least one Head of Family remains, and validates new members.
     *
     * @param request family update request containing modified attributes
     * @param existingFamily existing Family aggregate retrieved from the database
     * @throws CustomException if update validation constraints fail
     */
    public void validateUpdate(FamilyRequest request, Family existingFamily) {
        if (existingFamily == null) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, "Target family does not exist");
        }

        if (!FamilyStatus.ACTIVE.equals(existingFamily.getStatus())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "Cannot update family with status: " + existingFamily.getStatus());
        }

        Family family = request.getFamily();

        if (family.getMembers() != null && !family.getMembers().isEmpty()) {
            long headCount = family.getMembers().stream()
                    .filter(m -> Boolean.TRUE.equals(m.getIsHead()) && (m.getActive() == null || m.getActive()))
                    .count();

            if (headCount != 1) {
                throw new CustomException(FamilyConstants.ERROR_HEAD_OF_FAMILY_REQUIRED, FamilyConstants.MSG_HEAD_OF_FAMILY_MANDATORY);
            }

            for (FamilyMember member : family.getMembers()) {
                // Dynamic MDMS validation for relationshipWithHead
                if (StringUtils.isNotBlank(member.getRelationshipWithHead())) {
                    familyMasterDataService.validateRelationship(request.getRequestInfo(), existingFamily.getTenantId(), member.getRelationshipWithHead());
                }

                // Verify member is not already an active member in another active family
                if (Boolean.TRUE.equals(member.getActive()) || member.getActive() == null) {
                    List<String> activeFamilyIds = familyRepository.findActiveFamilyIdsByMemberUuid(member.getUserUuid());
                    if (activeFamilyIds != null && !activeFamilyIds.isEmpty() && !activeFamilyIds.contains(existingFamily.getFamilyId())) {
                        throw new CustomException(FamilyConstants.ERROR_MEMBER_ALREADY_EXISTS,
                                "Citizen profile " + member.getUserUuid() + " is already an active member of family: " + activeFamilyIds.get(0));
                    }
                }
            }
        }
    }

    /**
     * Validates a family split operation in UPYOG.
     * Ensures parent family is active, splitting members are currently active in the parent family,
     * and designated child family head is part of the splitting members.
     *
     * @param request split request payload
     * @param parentFamily existing parent Family aggregate
     * @throws CustomException if split constraints fail
     */
    public void validateSplit(FamilySplitRequest request, Family parentFamily) {
        if (parentFamily == null) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, "Parent family not found: " + request.getParentFamilyId());
        }

        if (!FamilyStatus.ACTIVE.equals(parentFamily.getStatus())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_SPLIT, "Parent family is not in ACTIVE state");
        }

        Set<String> parentMemberUuids = new HashSet<>();
        parentFamily.getMembers().stream()
                .filter(m -> Boolean.TRUE.equals(m.getActive()))
                .forEach(m -> parentMemberUuids.add(m.getUserUuid()));

        for (String splittingUuid : request.getSplittingMemberUserUuids()) {
            if (!parentMemberUuids.contains(splittingUuid)) {
                throw new CustomException(FamilyConstants.ERROR_INVALID_SPLIT,
                        "Member UUID " + splittingUuid + " is not an active member of parent family");
            }
        }

        if (!request.getSplittingMemberUserUuids().contains(request.getNewFamilyHeadUserUuid())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_SPLIT,
                    "New family Head of Family (" + request.getNewFamilyHeadUserUuid() + ") must be among the splitting members");
        }

        if (request.getSplittingMemberUserUuids().size() >= parentMemberUuids.size()) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_SPLIT,
                    "Cannot split all members from parent family. At least one member must remain in the parent unit.");
        }
    }

    /**
     * Validates a family merge operation in UPYOG.
     * Verifies that both surviving and merging families exist and are distinct, both are ACTIVE,
     * and the designated common head exists in the surviving family.
     *
     * @param request merge request payload
     * @param survivingFamily target surviving Family aggregate
     * @param mergingFamily source merging Family aggregate
     * @throws CustomException if merge constraints fail
     */
    public void validateMerge(FamilyMergeRequest request, Family survivingFamily, Family mergingFamily) {
        if (survivingFamily == null) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, "Surviving family not found: " + request.getSurvivingFamilyId());
        }
        if (mergingFamily == null) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, "Merging family not found: " + request.getMergingFamilyId());
        }

        if (survivingFamily.getFamilyId().equals(mergingFamily.getFamilyId())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_MERGE, "Cannot merge a family unit into itself");
        }

        if (!FamilyStatus.ACTIVE.equals(survivingFamily.getStatus())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_MERGE, "Surviving family is not ACTIVE");
        }
        if (!FamilyStatus.ACTIVE.equals(mergingFamily.getStatus())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_MERGE, "Merging family is not ACTIVE");
        }

        Set<String> allActiveMemberUuids = new HashSet<>();
        survivingFamily.getMembers().stream().filter(m -> Boolean.TRUE.equals(m.getActive())).forEach(m -> allActiveMemberUuids.add(m.getUserUuid()));
        mergingFamily.getMembers().stream().filter(m -> Boolean.TRUE.equals(m.getActive())).forEach(m -> allActiveMemberUuids.add(m.getUserUuid()));

        if (!allActiveMemberUuids.contains(request.getCommonHeadUserUuid())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_MERGE,
                    "Nominated common Head of Family (" + request.getCommonHeadUserUuid() + ") must belong to one of the merging families");
        }
    }
}
