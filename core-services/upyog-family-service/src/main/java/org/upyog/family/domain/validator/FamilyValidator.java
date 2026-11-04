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
 * Domain validator enforcing business rules, statutory invariants, and security policies for UPYOG Family management.
 *
 * <p>Validates:</p>
 * <ul>
 *   <li>Single active Head of Family per unit across creation and updates.</li>
 *   <li>Dynamic MDMS relationship validation against state taxonomy (e.g. {@code HEAD}, {@code SPOUSE}, {@code SON}).</li>
 *   <li>Multi-family exclusivity preventing duplicate active household memberships.</li>
 *   <li>FileStore accessibility for attached verification document IDs.</li>
 *   <li>Genealogical integrity for split operations (preserving at least one resident in parent).</li>
 *   <li>Distinctness and active state for household consolidation (merges).</li>
 * </ul>
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

    /**
     * Constructs {@link FamilyValidator} with repository, filestore, and master data dependencies.
     *
     * @param familyRepository data repository for family lookups and exclusivity queries
     * @param fileStoreClient client verifying document existence in FileStore
     * @param familyMasterDataService service providing MDMS relationship master data
     */
    @Autowired
    public FamilyValidator(FamilyRepository familyRepository,
                           FileStoreClient fileStoreClient,
                           FamilyMasterDataService familyMasterDataService) {
        this.familyRepository = familyRepository;
        this.fileStoreClient = fileStoreClient;
        this.familyMasterDataService = familyMasterDataService;
    }

    /**
     * Validates a family creation request against UPYOG business rules and integrity constraints.
     *
     * <p>Enforces:</p>
     * <ul>
     *   <li>Presence of non-blank {@code tenantId}.</li>
     *   <li>Exactly one member with {@code isHead = true} and aligns {@code family.headUserUuid}.</li>
     *   <li>Member user UUID uniqueness within the payload.</li>
     *   <li>Dynamic MDMS relationship codes.</li>
     *   <li>Multi-family exclusivity ensuring no member is already active in another family.</li>
     *   <li>FileStore accessibility for attached document IDs.</li>
     * </ul>
     *
     * @param request family creation request containing family metadata and members; must not be {@code null}.
     * @throws CustomException if any business invariant or structural validation check fails.
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

        if (StringUtils.isBlank(family.getHeadUserUuid())) {
            family.setHeadUserUuid(headMember.getUserUuid());
        } else if (!family.getHeadUserUuid().equals(headMember.getUserUuid())) {
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
     *
     * <p>Ensures:</p>
     * <ul>
     *   <li>Target family exists and is {@link FamilyStatus#ACTIVE}.</li>
     *   <li>Caller is the current Head of Family (or system user).</li>
     *   <li>Resulting member roster retains exactly one active HoF.</li>
     *   <li>Member relationships conform to MDMS taxonomy.</li>
     *   <li>Members are not active in any other family unit.</li>
     * </ul>
     *
     * @param request family update request containing modified attributes and caller identity; must not be {@code null}.
     * @param existingFamily existing Family aggregate retrieved from the database; must not be {@code null}.
     * @throws CustomException if target family is missing/inactive, caller is unauthorized, or business rules are violated.
     */
    public void validateUpdate(FamilyRequest request, Family existingFamily) {
        if (existingFamily == null) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, "Target family does not exist");
        }

        if (!FamilyStatus.ACTIVE.equals(existingFamily.getStatus())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "Cannot update family with status: " + existingFamily.getStatus());
        }

        // Caller authorization: Only current HoF is authorized to update family metadata
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null) {
            String callerUuid = request.getRequestInfo().getUserInfo().getUuid();
            if (StringUtils.isNotBlank(callerUuid) && !FamilyConstants.SYSTEM_USER.equalsIgnoreCase(callerUuid)) {
                if (!callerUuid.equals(existingFamily.getHeadUserUuid())) {
                    throw new CustomException(FamilyConstants.ERROR_UNAUTHORIZED_ACCESS,
                            "Only the Head of Family is authorized to update family metadata.");
                }
            }
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
     *
     * <p>Enforces:</p>
     * <ul>
     *   <li>Parent family exists and is in {@link FamilyStatus#ACTIVE} status.</li>
     *   <li>Caller is the authorized Head of Family of the parent family unit.</li>
     *   <li>All splitting member UUIDs belong to active residents of the parent unit.</li>
     *   <li>The designated new child unit head is among the splitting members.</li>
     *   <li>At least one member remains behind in the parent family unit.</li>
     * </ul>
     *
     * @param request split request payload containing parent ID, splitting members, and new head UUID; must not be {@code null}.
     * @param parentFamily existing parent {@link Family} aggregate; must not be {@code null}.
     * @throws CustomException if parent family is inactive, caller unauthorized, or splitting member rosters violate invariants.
     */
    public void validateSplit(FamilySplitRequest request, Family parentFamily) {
        if (parentFamily == null) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, "Parent family not found: " + request.getParentFamilyId());
        }

        if (!FamilyStatus.ACTIVE.equals(parentFamily.getStatus())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_SPLIT, "Parent family is not in ACTIVE state");
        }

        // Caller authorization: Only parent family HoF can initiate split
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null) {
            String callerUuid = request.getRequestInfo().getUserInfo().getUuid();
            if (StringUtils.isNotBlank(callerUuid) && !FamilyConstants.SYSTEM_USER.equalsIgnoreCase(callerUuid)) {
                if (!callerUuid.equals(parentFamily.getHeadUserUuid())) {
                    throw new CustomException(FamilyConstants.ERROR_UNAUTHORIZED_ACCESS,
                            "Only the parent family Head of Family is authorized to initiate a family split.");
                }
            }
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
     *
     * <p>Enforces:</p>
     * <ul>
     *   <li>Both surviving and merging family aggregates exist and are distinct.</li>
     *   <li>Both families are in {@link FamilyStatus#ACTIVE} status.</li>
     *   <li>Caller holds HoF authority over at least one of the participating families.</li>
     *   <li>The designated common Head of Family exists as an active member within the combined rosters.</li>
     * </ul>
     *
     * @param request merge request payload containing participating family IDs and common HoF UUID; must not be {@code null}.
     * @param survivingFamily target surviving {@link Family} aggregate; must not be {@code null}.
     * @param mergingFamily source merging {@link Family} aggregate; must not be {@code null}.
     * @throws CustomException if either family is missing or inactive, families are identical, or common HoF is invalid.
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

        // Caller authorization: Caller must be HoF of one of the merging family units
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null) {
            String callerUuid = request.getRequestInfo().getUserInfo().getUuid();
            if (StringUtils.isNotBlank(callerUuid) && !FamilyConstants.SYSTEM_USER.equalsIgnoreCase(callerUuid)) {
                if (!callerUuid.equals(survivingFamily.getHeadUserUuid()) && !callerUuid.equals(mergingFamily.getHeadUserUuid())) {
                    throw new CustomException(FamilyConstants.ERROR_UNAUTHORIZED_ACCESS,
                            "Only the Head of Family of one of the merging family units is authorized to merge families.");
                }
            }
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
