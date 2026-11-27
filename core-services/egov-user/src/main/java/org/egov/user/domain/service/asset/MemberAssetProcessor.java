package org.egov.user.domain.service.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.GuardianRelation;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.asset.MemberAssetDetail;
import org.egov.user.web.contract.asset.MemberAssetSyncRequest;
import org.egov.user.web.contract.asset.MemberAssetSyncResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * Transactional processor executing demographic and document synchronization for an individual family member.
 * Runs each member within an isolated {@code REQUIRES_NEW} transaction boundary so that partial failures
 * for one member cleanly roll back without corrupting database state or impacting other members in a batch.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class MemberAssetProcessor {

    private final UserRepository userRepository;
    private final UserDocumentRepository documentRepository;

    /**
     * Primary constructor injecting user and document persistence repositories.
     *
     * @param userRepository     repository for querying and updating user demographic records
     * @param documentRepository repository for persisting user documents and verification statuses
     */
    @Autowired
    public MemberAssetProcessor(UserRepository userRepository,
                                UserDocumentRepository documentRepository) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Processes demographic and document synchronization for an individual family member.
     * Runs in an isolated {@link Propagation#REQUIRES_NEW} transaction: any unhandled exception triggers
     * full rollback of changes made for this member, preventing partial commits.
     *
     * @param memberDetail member payload containing citizen UUID, demographics, and documents
     * @param request      original request context envelope
     * @return result object summarizing synced document counts and status
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public MemberAssetSyncResult processSingleMember(MemberAssetDetail memberDetail, MemberAssetSyncRequest request) {
        String userUuid = memberDetail.resolveUserUuid();
        log.info("[MEMBER-ASSET-PROCESSOR] Beginning isolated transaction demographic/document sync for citizen UUID: {}", userUuid);

        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .uuid(Collections.singletonList(userUuid))
                .excludeAddressDetails(true)
                .build();
        List<User> foundUsers = userRepository.findAll(criteria);
        if (CollectionUtils.isEmpty(foundUsers)) {
            log.warn("[MEMBER-ASSET-PROCESSOR] Citizen with UUID {} not found in eg_user", userUuid);
            return MemberAssetSyncResult.builder()
                    .userUuid(userUuid)
                    .status(UserConstants.STATUS_FAILED)
                    .message(UserConstants.MSG_USER_RECORD_NOT_FOUND_PREFIX + userUuid + UserConstants.MSG_USER_RECORD_NOT_FOUND_SUFFIX)
                    .build();
        }

        User existingUser = foundUsers.get(0);

        if (StringUtils.isNotBlank(memberDetail.getTenantId()) && StringUtils.isNotBlank(existingUser.getTenantId())) {
            String memberTenant = memberDetail.getTenantId().trim();
            String userTenant = existingUser.getTenantId().trim();
            if (!isTenantCompatible(memberTenant, userTenant)) {
                log.warn("[MEMBER-ASSET-PROCESSOR] Tenant mismatch for citizen UUID {}: payload tenant '{}' does not match user tenant '{}'",
                        userUuid, memberTenant, userTenant);
                return MemberAssetSyncResult.builder()
                        .userUuid(userUuid)
                        .status(UserConstants.STATUS_FAILED)
                        .message("Tenant ID mismatch: " + memberTenant + " is incompatible with user record tenant " + userTenant)
                        .build();
            }
        }

        User oldUser = existingUser.toBuilder().build();

        AuditActor actor = resolveAuditActor(request);
        Long actorId = actor.getId();
        String actorUuid = actor.getUuid();

        // 1. Update demographics on eg_user
        boolean demographicModified = updateDemographics(existingUser, memberDetail);
        if (demographicModified) {
            userRepository.update(existingUser, oldUser, actorId, actorUuid);
            log.info("[MEMBER-ASSET-PROCESSOR] Updated demographics in eg_user for UUID: {}", userUuid);
        }

        // 2. Persist attached citizen documents (e.g. Caste certificate, Aadhaar, DOB proof)
        int docsSynced = persistCitizenDocuments(memberDetail, existingUser, actorUuid);

        log.info("[MEMBER-ASSET-PROCESSOR] Successfully completed demographic/document sync for citizen UUID: {}. Documents: {}",
                userUuid, docsSynced);

        return MemberAssetSyncResult.builder()
                .userUuid(userUuid)
                .status(UserConstants.STATUS_SUCCESS)
                .message(UserConstants.MSG_MEMBER_ASSETS_SYNC_SUCCESS)
                .documentsSynced(docsSynced)
                .build();
    }

    /**
     * Updates mutable citizen demographic fields on the existing user entity if changes are present.
     *
     * @param existingUser the existing citizen user domain entity
     * @param detail       the incoming member detail
     * @return {@code true} if at least one demographic field was modified; {@code false} otherwise
     */
    private boolean updateDemographics(User existingUser, MemberAssetDetail detail) {
        boolean modified = false;

        String fatherName = detail.resolveFatherName();
        if (StringUtils.isNotBlank(fatherName) && !StringUtils.equals(fatherName, existingUser.getFatherName())) {
            existingUser.setFatherName(fatherName);
            modified = true;
        }

        String motherName = detail.resolveMotherName();
        if (StringUtils.isNotBlank(motherName) && !StringUtils.equals(motherName, existingUser.getMotherName())) {
            existingUser.setMotherName(motherName);
            modified = true;
        }

        String spouseName = detail.resolveSpouseName();
        if (StringUtils.isNotBlank(spouseName) && !StringUtils.equals(spouseName, existingUser.getSpouseName())) {
            existingUser.setSpouseName(spouseName);
            modified = true;
        }

        String guardian = detail.resolveGuardian();
        if (StringUtils.isNotBlank(guardian) && !StringUtils.equals(guardian, existingUser.getGuardian())) {
            existingUser.setGuardian(guardian);
            modified = true;
        }

        String guardianRelationStr = detail.resolveGuardianRelation();
        if (StringUtils.isNotBlank(guardianRelationStr)) {
            try {
                GuardianRelation gr = GuardianRelation.valueOf(guardianRelationStr.trim().toUpperCase());
                if (gr != existingUser.getGuardianRelation()) {
                    existingUser.setGuardianRelation(gr);
                    modified = true;
                }
            } catch (Exception ignored) {
                // Ignore unrecognized guardian relation string
            }
        }

        String caste = detail.resolveCaste();
        if (StringUtils.isNotBlank(caste) && !StringUtils.equals(caste, existingUser.getCaste())) {
            existingUser.setCaste(caste);
            modified = true;
        }

        Boolean speciallyAbled = detail.resolveIsSpeciallyAbled();
        if (speciallyAbled != null && !speciallyAbled.equals(existingUser.getIsSpeciallyAbled())) {
            existingUser.setIsSpeciallyAbled(speciallyAbled);
            modified = true;
        }

        String altMobile = detail.resolveAlternateMobileNumber();
        if (StringUtils.isNotBlank(altMobile) && !StringUtils.equals(altMobile, existingUser.getAlternateMobileNumber())) {
            existingUser.setAlternateMobileNumber(altMobile);
            modified = true;
        }

        String email = detail.resolveEmailId();
        if (StringUtils.isNotBlank(email) && !StringUtils.equals(email, existingUser.getEmailId())) {
            existingUser.setEmailId(email);
            modified = true;
        }

        return modified;
    }

    /**
     * Persists citizen identity and proof documents into {@code ug_user_document}.
     *
     * @param detail       member detail containing attached documents
     * @param existingUser existing citizen user
     * @param actorUuid    UUID of the updater
     * @return count of successfully persisted documents
     */
    private int persistCitizenDocuments(MemberAssetDetail detail, User existingUser, String actorUuid) {
        List<UserDocument> documents = detail.resolveAllDocuments();
        if (CollectionUtils.isEmpty(documents)) {
            return 0;
        }

        int count = 0;
        for (UserDocument doc : documents) {
            if (doc == null || StringUtils.isBlank(doc.getFilestoreId())) {
                continue;
            }
            doc.setUserId(existingUser.getId());
            doc.setUserUuid(existingUser.getUuid());
            if (StringUtils.isBlank(doc.getTenantId())) {
                doc.setTenantId(detail.getTenantId() != null ? detail.getTenantId() : existingUser.getTenantId());
            }
            if (StringUtils.isBlank(doc.getReferenceType())) {
                doc.setReferenceType(UserConstants.DOC_REF_TYPE_CITIZEN_PROFILE);
            }
            if (StringUtils.isBlank(doc.getReferenceId())) {
                doc.setReferenceId(existingUser.getUuid());
            }
            doc.setCreatedBy(actorUuid);
            doc.setLastModifiedBy(actorUuid);
            documentRepository.create(doc);
            count++;
        }
        return count;
    }

    /**
     * Checks if member tenant and existing user tenant are compatible (share the same state tenant root).
     *
     * @param memberTenant tenant ID supplied in member payload
     * @param userTenant   tenant ID on persisted eg_user record
     * @return true if compatible or if either is blank, false otherwise
     */
    private boolean isTenantCompatible(String memberTenant, String userTenant) {
        if (StringUtils.isBlank(memberTenant) || StringUtils.isBlank(userTenant)) {
            return true;
        }
        String rootMemberTenant = memberTenant.contains(".") ? memberTenant.split("\\.")[0] : memberTenant;
        String rootUserTenant = userTenant.contains(".") ? userTenant.split("\\.")[0] : userTenant;
        return rootMemberTenant.equalsIgnoreCase(rootUserTenant);
    }

    /**
     * Resolves the audit actor numeric ID and UUID strictly from {@code RequestInfo.userInfo}.
     *
     * @param request synchronization request containing request context
     * @return resolved audit actor credentials
     * @throws CustomException if {@code requestInfo}, {@code userInfo}, or {@code uuid} is missing
     */
    private AuditActor resolveAuditActor(MemberAssetSyncRequest request) {
        if (request == null || request.getRequestInfo() == null || request.getRequestInfo().getUserInfo() == null
                || StringUtils.isBlank(request.getRequestInfo().getUserInfo().getUuid())
                || request.getRequestInfo().getUserInfo().getId() == null) {
            log.error("[MEMBER-ASSET-PROCESSOR] Caller userInfo (both id and uuid) is mandatory in RequestInfo for member synchronization");
            throw new CustomException(UserConstants.ERROR_CALLER_INFO_REQUIRED, UserConstants.MSG_CALLER_INFO_REQUIRED);
        }

        var userInfo = request.getRequestInfo().getUserInfo();
        String actorUuid = userInfo.getUuid().trim();
        Long actorId = userInfo.getId();

        return new AuditActor(actorId, actorUuid);
    }

    /**
     * Immutable value object holding resolved audit actor credentials.
     */
    @Getter
    @AllArgsConstructor
    private static class AuditActor {
        private final Long id;
        private final String uuid;
    }
}
