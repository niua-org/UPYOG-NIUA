package org.egov.user.domain.service.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.asset.PropertyOwner;
import org.egov.user.domain.model.asset.RuralPropertyDetail;
import org.egov.user.domain.model.asset.UrbanPropertyDetail;
import org.egov.user.domain.model.asset.UserIncome;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.domain.model.asset.UserVehicle;
import org.egov.user.domain.model.enums.GuardianRelation;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserIncomeRepository;
import org.egov.user.persistence.repository.UserPropertyRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.persistence.repository.UserVehicleRepository;
import org.egov.user.web.contract.asset.MemberAssetDetail;
import org.egov.user.web.contract.asset.MemberAssetSyncRequest;
import org.egov.user.web.contract.asset.MemberAssetSyncResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Transactional processor executing asset and demographic synchronization for an individual family member.
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
    private final UserPropertyRepository propertyRepository;
    private final UserIncomeRepository incomeRepository;
    private final UserVehicleRepository vehicleRepository;
    private final UserDocumentRepository documentRepository;
    private final UserConfiguration userConfiguration;

    /**
     * Primary constructor injecting persistence repositories and centralized configuration.
     *
     * @param userRepository     repository for querying and updating user demographic records
     * @param propertyRepository repository for persisting 3NF property assets and ownership
     * @param incomeRepository   repository for persisting citizen income declarations
     * @param vehicleRepository  repository for persisting declared vehicle assets
     * @param documentRepository repository for persisting user documents and verification statuses
     * @param userConfiguration  centralized configuration for system actor audit constants
     */
    @Autowired
    public MemberAssetProcessor(UserRepository userRepository,
                                UserPropertyRepository propertyRepository,
                                UserIncomeRepository incomeRepository,
                                UserVehicleRepository vehicleRepository,
                                UserDocumentRepository documentRepository,
                                @Autowired(required = false) UserConfiguration userConfiguration) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.incomeRepository = incomeRepository;
        this.vehicleRepository = vehicleRepository;
        this.documentRepository = documentRepository;
        this.userConfiguration = userConfiguration;
    }

    /**
     * Backwards-compatible constructor for testing and manual wiring.
     */
    public MemberAssetProcessor(UserRepository userRepository,
                                UserPropertyRepository propertyRepository,
                                UserIncomeRepository incomeRepository,
                                UserVehicleRepository vehicleRepository,
                                UserDocumentRepository documentRepository) {
        this(userRepository, propertyRepository, incomeRepository, vehicleRepository, documentRepository, null);
    }

    /**
     * Processes asset and demographic synchronization for an individual family member.
     * Runs in an isolated {@link Propagation#REQUIRES_NEW} transaction: any unhandled exception triggers
     * full rollback of changes made for this member, preventing partial commits.
     *
     * @param memberDetail member payload containing citizen UUID, demographics, and assets
     * @param request      original request context envelope
     * @return result object summarizing synced asset counts and status
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public MemberAssetSyncResult processSingleMember(MemberAssetDetail memberDetail, MemberAssetSyncRequest request) {
        String userUuid = memberDetail.resolveUserUuid();
        log.info("[MEMBER-ASSET-PROCESSOR] Beginning isolated transaction asset synchronization for citizen UUID: {}", userUuid);

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

        // Security check: Validate tenant compatibility between payload and existing citizen record to prevent cross-tenant overwrites
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

        // Fix for PR Comment #4 (Audit actor fallback):
        // Resolve actor credentials strictly from RequestInfo.userInfo.
        // Under NO circumstances fall back to the synchronized citizen's own ID or UUID,
        // which previously corrupted the audit trail (createdby/lastmodifiedby).
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
        int docsSynced = persistCitizenDocuments(memberDetail, existingUser, actorId);

        // 3. Persist declared property assets (Rural and Urban with co-owners)
        List<String> propertyIds = persistProperties(memberDetail, existingUser, actorId, actorUuid);

        // 4. Persist income and fiscal identity declarations
        String incomeId = persistIncome(memberDetail, existingUser, actorId, actorUuid);

        // 5. Persist vehicle declarations
        List<String> vehicleIds = persistVehicles(memberDetail, existingUser, actorId, actorUuid);

        log.info("[MEMBER-ASSET-PROCESSOR] Successfully completed asset sync for citizen UUID: {}. Properties: {}, Vehicles: {}, Income: {}",
                userUuid, propertyIds.size(), vehicleIds.size(), incomeId != null);

        return MemberAssetSyncResult.builder()
                .userUuid(userUuid)
                .status(UserConstants.STATUS_SUCCESS)
                .message(UserConstants.MSG_MEMBER_ASSETS_SYNC_SUCCESS)
                .propertiesSynced(propertyIds.size())
                .propertyIds(propertyIds)
                .incomeSynced(incomeId != null)
                .incomeId(incomeId)
                .vehiclesSynced(vehicleIds.size())
                .vehicleIds(vehicleIds)
                .documentsSynced(docsSynced)
                .build();
    }

    /**
     * Updates mutable citizen demographic fields on the existing user entity if changes are present.
     *
     * @param existingUser the existing citizen user domain entity
     * @param detail       the incoming member asset detail
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
     * @param detail       member asset detail containing attached documents
     * @param existingUser existing citizen user
     * @param actorId      numeric user ID of the updater
     * @return count of successfully persisted documents
     */
    private int persistCitizenDocuments(MemberAssetDetail detail, User existingUser, Long actorId) {
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
            doc.setCreatedBy(actorId);
            doc.setLastModifiedBy(actorId);
            documentRepository.create(doc);
            count++;
        }
        return count;
    }

    /**
     * Persists declared properties, urban/rural specializations, and co-owners into normalized 3NF tables.
     *
     * @param detail       member asset detail containing property list
     * @param existingUser existing citizen user
     * @param actorId      numeric ID of the updater
     * @param actorUuid    UUID of the updater
     * @return list of generated property primary key IDs
     */
    private List<String> persistProperties(MemberAssetDetail detail, User existingUser, Long actorId, String actorUuid) {
        List<UserProperty> properties = detail.getProperties();
        if (CollectionUtils.isEmpty(properties)) {
            return Collections.emptyList();
        }

        List<String> propertyIds = new ArrayList<>();
        String effectiveTenant = StringUtils.isNotBlank(detail.getTenantId()) ? detail.getTenantId() : existingUser.getTenantId();

        for (UserProperty prop : properties) {
            if (prop == null) continue;

            // Natural key deduplication: if id is blank, attempt to reuse existing property record ID
            if (StringUtils.isBlank(prop.getId())) {
                if (prop.getUrbanPropertyDetail() != null && StringUtils.isNotBlank(prop.getUrbanPropertyDetail().getUpyogPropertyId())) {
                    String existingId = propertyRepository.findUrbanPropertyId(existingUser.getUuid(), prop.getUrbanPropertyDetail().getUpyogPropertyId());
                    if (existingId != null) {
                        prop.setId(existingId);
                    }
                } else if (prop.getRuralPropertyDetail() != null && StringUtils.isNotBlank(prop.getRuralPropertyDetail().getSurveyNumber())) {
                    String existingId = propertyRepository.findRuralPropertyId(existingUser.getUuid(), prop.getRuralPropertyDetail().getSurveyNumber());
                    if (existingId != null) {
                        prop.setId(existingId);
                    }
                }
                if (StringUtils.isBlank(prop.getId())) {
                    prop.setId(UUID.randomUUID().toString());
                }
            } else {
                // Security check: verify client-supplied property ID does not belong to another citizen
                String existingOwner = propertyRepository.findUserUuidByPropertyId(prop.getId());
                if (existingOwner != null && !existingOwner.equals(existingUser.getUuid())) {
                    log.warn("[MEMBER-ASSET-PROCESSOR] Cross-user property access blocked. Property {} belongs to {}, caller supplied citizen {}",
                            prop.getId(), existingOwner, existingUser.getUuid());
                    prop.setId(UUID.randomUUID().toString());
                }
            }
            prop.setUserUuid(existingUser.getUuid());
            if (StringUtils.isBlank(prop.getTenantId())) {
                prop.setTenantId(effectiveTenant);
            }
            prop.setCreatedBy(actorUuid);
            prop.setLastModifiedBy(actorUuid);

            // Document proof attachment
            if (prop.getDocument() != null && StringUtils.isNotBlank(prop.getDocument().getFilestoreId())) {
                UserDocument doc = prop.getDocument();
                doc.setUserId(existingUser.getId());
                doc.setUserUuid(existingUser.getUuid());
                doc.setTenantId(prop.getTenantId());
                doc.setReferenceType(UserConstants.DOC_REF_TYPE_PROPERTY);
                doc.setReferenceId(prop.getId());
                doc.setCreatedBy(actorId);
                doc.setLastModifiedBy(actorId);
                documentRepository.create(doc);
                prop.setDocumentId(doc.getId());
            }

            propertyRepository.saveProperty(prop);
            propertyIds.add(prop.getId());

            // Urban property specialization
            if (UserConstants.LOCALITY_TYPE_URBAN.equalsIgnoreCase(prop.getLocalityType()) && prop.getUrbanPropertyDetail() != null) {
                UrbanPropertyDetail urban = prop.getUrbanPropertyDetail();
                urban.setPropertyRecordId(prop.getId());
                urban.setTenantId(prop.getTenantId());
                urban.setCreatedBy(actorUuid);
                urban.setLastModifiedBy(actorUuid);
                propertyRepository.saveUrbanProperty(urban);
            }

            // Rural property specialization
            if (UserConstants.LOCALITY_TYPE_RURAL.equalsIgnoreCase(prop.getLocalityType()) && prop.getRuralPropertyDetail() != null) {
                RuralPropertyDetail rural = prop.getRuralPropertyDetail();
                rural.setPropertyRecordId(prop.getId());
                rural.setTenantId(prop.getTenantId());
                rural.setCreatedBy(actorUuid);
                rural.setLastModifiedBy(actorUuid);
                propertyRepository.saveRuralProperty(rural);
            }

            // Normalized co-owners (strictly 3NF)
            if (!CollectionUtils.isEmpty(prop.getOwners())) {
                for (PropertyOwner owner : prop.getOwners()) {
                    if (owner == null) continue;
                    if (StringUtils.isBlank(owner.getId())) {
                        owner.setId(UUID.randomUUID().toString());
                    }
                    owner.setPropertyId(prop.getId());
                    owner.setTenantId(prop.getTenantId());
                    owner.setCreatedBy(actorUuid);
                    owner.setLastModifiedBy(actorUuid);
                    propertyRepository.savePropertyOwner(owner);
                }
            }
        }
        return propertyIds;
    }

    /**
     * Persists fiscal income declarations and attached tax/salary proof documents into {@code ug_user_income}.
     *
     * @param detail       member asset detail containing income declaration
     * @param existingUser existing citizen user
     * @param actorId      numeric ID of the updater
     * @param actorUuid    UUID of the updater
     * @return generated income primary key ID or null if no income was declared
     */
    private String persistIncome(MemberAssetDetail detail, User existingUser, Long actorId, String actorUuid) {
        UserIncome income = detail.getIncome();
        if (income == null) {
            return null;
        }

        // Natural key deduplication: if id is blank, reuse existing active income record ID for this citizen
        if (StringUtils.isBlank(income.getId())) {
            String existingId = incomeRepository.findActiveIncomeIdByUserUuid(existingUser.getUuid());
            if (existingId != null) {
                income.setId(existingId);
            } else {
                income.setId(UUID.randomUUID().toString());
            }
        } else {
            // Security check: verify client-supplied income ID does not belong to another citizen
            String existingOwner = incomeRepository.findUserUuidByIncomeId(income.getId());
            if (existingOwner != null && !existingOwner.equals(existingUser.getUuid())) {
                log.warn("[MEMBER-ASSET-PROCESSOR] Cross-user income access blocked. Income {} belongs to {}, caller supplied citizen {}",
                        income.getId(), existingOwner, existingUser.getUuid());
                income.setId(UUID.randomUUID().toString());
            }
        }
        income.setUserUuid(existingUser.getUuid());
        if (StringUtils.isBlank(income.getTenantId())) {
            income.setTenantId(StringUtils.isNotBlank(detail.getTenantId()) ? detail.getTenantId() : existingUser.getTenantId());
        }
        income.setCreatedBy(actorUuid);
        income.setLastModifiedBy(actorUuid);

        // Salary proof document
        if (income.getSalaryProofDocument() != null && StringUtils.isNotBlank(income.getSalaryProofDocument().getFilestoreId())) {
            UserDocument sDoc = income.getSalaryProofDocument();
            sDoc.setUserId(existingUser.getId());
            sDoc.setUserUuid(existingUser.getUuid());
            sDoc.setTenantId(income.getTenantId());
            sDoc.setReferenceType(UserConstants.DOC_REF_TYPE_INCOME_SALARY);
            sDoc.setReferenceId(income.getId());
            sDoc.setCreatedBy(actorId);
            sDoc.setLastModifiedBy(actorId);
            documentRepository.create(sDoc);
            income.setSalaryProofDocId(sDoc.getId());
        }

        // Tax proof document
        if (income.getTaxProofDocument() != null && StringUtils.isNotBlank(income.getTaxProofDocument().getFilestoreId())) {
            UserDocument tDoc = income.getTaxProofDocument();
            tDoc.setUserId(existingUser.getId());
            tDoc.setUserUuid(existingUser.getUuid());
            tDoc.setTenantId(income.getTenantId());
            tDoc.setReferenceType(UserConstants.DOC_REF_TYPE_INCOME_TAX);
            tDoc.setReferenceId(income.getId());
            tDoc.setCreatedBy(actorId);
            tDoc.setLastModifiedBy(actorId);
            documentRepository.create(tDoc);
            income.setTaxProofDocId(tDoc.getId());
        }

        incomeRepository.saveIncome(income);
        return income.getId();
    }

    /**
     * Persists declared vehicles and RC proof documents into {@code ug_user_vehicle}.
     *
     * @param detail       member asset detail containing vehicle declarations
     * @param existingUser existing citizen user
     * @param actorId      numeric ID of the updater
     * @param actorUuid    UUID of the updater
     * @return list of generated vehicle primary key IDs
     */
    private List<String> persistVehicles(MemberAssetDetail detail, User existingUser, Long actorId, String actorUuid) {
        List<UserVehicle> vehicles = detail.getVehicles();
        if (CollectionUtils.isEmpty(vehicles)) {
            return Collections.emptyList();
        }

        List<String> vehicleIds = new ArrayList<>();
        String effectiveTenant = StringUtils.isNotBlank(detail.getTenantId()) ? detail.getTenantId() : existingUser.getTenantId();

        for (UserVehicle veh : vehicles) {
            if (veh == null) continue;

            // Natural key deduplication: if id is blank, reuse existing active vehicle by vehicle registration number
            if (StringUtils.isBlank(veh.getId())) {
                if (StringUtils.isNotBlank(veh.getVehicleNumber())) {
                    String existingId = vehicleRepository.findVehicleIdByUserAndNumber(existingUser.getUuid(), veh.getVehicleNumber());
                    if (existingId != null) {
                        veh.setId(existingId);
                    }
                }
                if (StringUtils.isBlank(veh.getId())) {
                    veh.setId(UUID.randomUUID().toString());
                }
            } else {
                // Security check: verify client-supplied vehicle ID does not belong to another citizen
                String existingOwner = vehicleRepository.findUserUuidByVehicleId(veh.getId());
                if (existingOwner != null && !existingOwner.equals(existingUser.getUuid())) {
                    log.warn("[MEMBER-ASSET-PROCESSOR] Cross-user vehicle access blocked. Vehicle {} belongs to {}, caller supplied citizen {}",
                            veh.getId(), existingOwner, existingUser.getUuid());
                    veh.setId(UUID.randomUUID().toString());
                }
            }
            veh.setUserUuid(existingUser.getUuid());
            if (StringUtils.isBlank(veh.getTenantId())) {
                veh.setTenantId(effectiveTenant);
            }
            veh.setCreatedBy(actorUuid);
            veh.setLastModifiedBy(actorUuid);

            // Registration Certificate (RC) document
            if (veh.getRcDocument() != null && StringUtils.isNotBlank(veh.getRcDocument().getFilestoreId())) {
                UserDocument rcDoc = veh.getRcDocument();
                rcDoc.setUserId(existingUser.getId());
                rcDoc.setUserUuid(existingUser.getUuid());
                rcDoc.setTenantId(veh.getTenantId());
                rcDoc.setReferenceType(UserConstants.DOC_REF_TYPE_VEHICLE_RC);
                rcDoc.setReferenceId(veh.getId());
                rcDoc.setCreatedBy(actorId);
                rcDoc.setLastModifiedBy(actorId);
                documentRepository.create(rcDoc);
                veh.setRcDocId(rcDoc.getId());
            }

            vehicleRepository.saveVehicle(veh);
            vehicleIds.add(veh.getId());
        }
        return vehicleIds;
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
     * Resolves the audit actor numeric ID and UUID strictly from {@link RequestInfo#getUserInfo()} or {@link UserConfiguration}.
     * Fixes PR Comment #4 by guaranteeing that audit columns ({@code createdby}, {@code lastmodifiedby})
     * are never attributed to the target citizen whose assets are being synchronized, and eliminates
     * inline fallback default values.
     *
     * <p>If caller details are absent in {@code RequestInfo}:</p>
     * <ul>
     *   <li>If {@code user.member.asset.sync.require.caller=true}, rejects the request with {@code CALLER_INFO_REQUIRED}.</li>
     *   <li>Otherwise, retrieves configured system actor credentials from {@link UserConfiguration}.</li>
     * </ul>
     *
     * @param request synchronization request containing request context
     * @return resolved audit actor credentials
     */
    private AuditActor resolveAuditActor(MemberAssetSyncRequest request) {
        Long configuredSystemId = (userConfiguration != null) ? userConfiguration.getSystemActorId() : null;
        String configuredSystemUuid = (userConfiguration != null) ? userConfiguration.getSystemActorUuid() : null;
        boolean requireCaller = userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getRequireCallerForSync());

        if (request == null || request.getRequestInfo() == null || request.getRequestInfo().getUserInfo() == null) {
            if (requireCaller) {
                log.error("[MEMBER-ASSET-PROCESSOR] Caller userInfo is missing in RequestInfo and requireCallerForSync is enforced");
                throw new CustomException(UserConstants.ERROR_CALLER_INFO_REQUIRED, UserConstants.MSG_CALLER_INFO_REQUIRED);
            }
            log.info("[MEMBER-ASSET-PROCESSOR] RequestInfo.userInfo is missing caller details. Using configured system actor: id={}, uuid={}",
                    configuredSystemId, configuredSystemUuid);
            return new AuditActor(configuredSystemId, configuredSystemUuid);
        }

        var userInfo = request.getRequestInfo().getUserInfo();
        Long actorId = userInfo.getId() != null ? userInfo.getId() : configuredSystemId;
        String actorUuid = StringUtils.isNotBlank(userInfo.getUuid()) ? userInfo.getUuid() : configuredSystemUuid;

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
