package org.egov.user.domain.service.asset;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
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
import org.egov.user.web.contract.asset.MemberAssetSyncResponse;
import org.egov.user.web.contract.asset.MemberAssetSyncResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link MemberAssetService} orchestrating transactional synchronization of
 * citizen profile demographics, attached KYC/identity proofs, properties, income, and vehicle declarations.
 *
 * <p>Strictly adheres to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Coordinates cross-table persistence across user and asset repositories.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> Extensible to further asset specializations without modifying contract shapes.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Relies strictly on injected repository abstractions.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class MemberAssetServiceImpl implements MemberAssetService {

    private final UserRepository userRepository;
    private final UserPropertyRepository propertyRepository;
    private final UserIncomeRepository incomeRepository;
    private final UserVehicleRepository vehicleRepository;
    private final UserDocumentRepository documentRepository;

    /**
     * Constructs a {@code MemberAssetServiceImpl} with repositories for users, properties, income, vehicles, and documents.
     *
     * @param userRepository repository for querying and updating user demographic records
     * @param propertyRepository repository for persisting 3NF property assets and ownership
     * @param incomeRepository repository for persisting citizen income declarations
     * @param vehicleRepository repository for persisting declared vehicle assets
     * @param documentRepository repository for persisting user documents and verification statuses
     */
    @Autowired
    public MemberAssetServiceImpl(UserRepository userRepository,
                                 UserPropertyRepository propertyRepository,
                                 UserIncomeRepository incomeRepository,
                                 UserVehicleRepository vehicleRepository,
                                 UserDocumentRepository documentRepository) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.incomeRepository = incomeRepository;
        this.vehicleRepository = vehicleRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Synchronizes demographics and declared assets for all members in the incoming sync request.
     *
     * @param request the member asset sync request payload
     * @return synchronization response envelope detailing per-member results
     * @throws CustomException if request payload is null
     */
    @Override
    @Transactional
    public MemberAssetSyncResponse syncMemberAssets(MemberAssetSyncRequest request) {
        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_REQUEST, UserConstants.MSG_MEMBER_ASSET_SYNC_PAYLOAD_NULL);
        }

        List<MemberAssetDetail> members = request.getNormalizedMembers();
        if (CollectionUtils.isEmpty(members)) {
            log.warn("[MEMBER-ASSET-SERVICE] Received empty member list for synchronization");
            return MemberAssetSyncResponse.builder()
                    .responseInfo(ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build())
                    .status(UserConstants.STATUS_SUCCESS)
                    .results(Collections.emptyList())
                    .build();
        }

        List<MemberAssetSyncResult> results = new ArrayList<>();

        for (MemberAssetDetail memberDetail : members) {
            String userUuid = memberDetail.resolveUserUuid();
            if (StringUtils.isBlank(userUuid)) {
                log.error("[MEMBER-ASSET-SERVICE] Missing userUuid for member payload: {}", memberDetail);
                results.add(MemberAssetSyncResult.builder()
                        .userUuid(UserConstants.UNKNOWN_USER_UUID)
                        .status(UserConstants.STATUS_FAILED)
                        .message(UserConstants.MSG_CITIZEN_USER_UUID_REQUIRED)
                        .build());
                continue;
            }

            try {
                MemberAssetSyncResult result = processSingleMember(memberDetail, request);
                results.add(result);
            } catch (Exception e) {
                log.error("[MEMBER-ASSET-SERVICE] Synchronization failed for user UUID {}: {}", userUuid, e.getMessage(), e);
                results.add(MemberAssetSyncResult.builder()
                        .userUuid(userUuid)
                        .status(UserConstants.STATUS_FAILED)
                        .message(UserConstants.MSG_SYNC_ERROR_PREFIX + e.getMessage())
                        .build());
            }
        }

        return MemberAssetSyncResponse.builder()
                .responseInfo(ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build())
                .status(UserConstants.STATUS_SUCCESS)
                .results(results)
                .build();
    }

    /**
     * Processes asset and demographic synchronization for an individual family member.
     *
     * @param memberDetail member payload containing citizen UUID, demographics, and assets
     * @param request      original request context envelope
     * @return result object summarizing synced asset counts and status
     */
    private MemberAssetSyncResult processSingleMember(MemberAssetDetail memberDetail, MemberAssetSyncRequest request) {
        String userUuid = memberDetail.resolveUserUuid();
        log.info("[MEMBER-ASSET-SERVICE] Beginning asset synchronization for citizen UUID: {}", userUuid);

        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .uuid(Collections.singletonList(userUuid))
                .excludeAddressDetails(true)
                .build();
        List<User> foundUsers = userRepository.findAll(criteria);
        if (CollectionUtils.isEmpty(foundUsers)) {
            log.warn("[MEMBER-ASSET-SERVICE] Citizen with UUID {} not found in eg_user", userUuid);
            return MemberAssetSyncResult.builder()
                    .userUuid(userUuid)
                    .status(UserConstants.STATUS_FAILED)
                    .message(UserConstants.MSG_USER_RECORD_NOT_FOUND_PREFIX + userUuid + UserConstants.MSG_USER_RECORD_NOT_FOUND_SUFFIX)
                    .build();
        }

        User existingUser = foundUsers.get(0);
        User oldUser = existingUser.toBuilder().build();
        Long userId = existingUser.getId();

        Long actorId = (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null && request.getRequestInfo().getUserInfo().getId() != null)
                ? request.getRequestInfo().getUserInfo().getId()
                : (userId != null ? userId : 1L);

        String actorUuid = (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null && StringUtils.isNotBlank(request.getRequestInfo().getUserInfo().getUuid()))
                ? request.getRequestInfo().getUserInfo().getUuid()
                : existingUser.getUuid();

        // 1. Update demographics on eg_user
        boolean demographicModified = updateDemographics(existingUser, memberDetail);
        if (demographicModified) {
            userRepository.update(existingUser, oldUser, actorId, actorUuid);
            log.info("[MEMBER-ASSET-SERVICE] Updated demographics in eg_user for UUID: {}", userUuid);
        }

        // 2. Persist attached citizen documents (e.g. Caste certificate, Aadhaar, DOB proof)
        int docsSynced = persistCitizenDocuments(memberDetail, existingUser, actorId);

        // 3. Persist declared property assets (Rural and Urban with co-owners)
        List<String> propertyIds = persistProperties(memberDetail, existingUser, actorId, actorUuid);

        // 4. Persist income and fiscal identity declarations
        String incomeId = persistIncome(memberDetail, existingUser, actorId, actorUuid);

        // 5. Persist vehicle declarations
        List<String> vehicleIds = persistVehicles(memberDetail, existingUser, actorId, actorUuid);

        log.info("[MEMBER-ASSET-SERVICE] Successfully completed asset sync for citizen UUID: {}. Properties: {}, Vehicles: {}, Income: {}",
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

        String guardianRelation = detail.resolveGuardianRelation();
        if (StringUtils.isNotBlank(guardianRelation)) {
            try {
                GuardianRelation rel = GuardianRelation.valueOf(guardianRelation.trim().toUpperCase());
                if (!rel.equals(existingUser.getGuardianRelation())) {
                    existingUser.setGuardianRelation(rel);
                    modified = true;
                }
            } catch (Exception e) {
                log.debug("[MEMBER-ASSET-SERVICE] Unrecognized guardian relation: {}", guardianRelation);
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

        String photo = detail.resolvePhoto();
        if (StringUtils.isNotBlank(photo) && !StringUtils.equals(photo, existingUser.getPhoto())) {
            existingUser.setPhoto(photo);
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

            if (StringUtils.isBlank(prop.getId())) {
                prop.setId(UUID.randomUUID().toString());
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

        if (StringUtils.isBlank(income.getId())) {
            income.setId(UUID.randomUUID().toString());
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

            if (StringUtils.isBlank(veh.getId())) {
                veh.setId(UUID.randomUUID().toString());
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
}
