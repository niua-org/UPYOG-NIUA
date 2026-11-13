package org.upyog.family.domain.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.service.config.FamilyConfigurationService;
import org.upyog.family.domain.service.master.FamilyMasterDataService;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.FileStoreClient;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyDocument;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilyMergeRequest;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.FamilySplitRequest;
import org.upyog.family.web.contract.RuralPropertyDetail;
import org.upyog.family.web.contract.UrbanPropertyDetail;
import org.upyog.family.web.contract.UserDocument;
import org.upyog.family.web.contract.UserIncomeDetail;
import org.upyog.family.web.contract.UserProperty;
import org.upyog.family.web.contract.UserVehicleDetail;
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
    private final UserClient userClient;
    private final FamilyConfigurationService familyConfigurationService;
    private final org.upyog.family.config.FamilyConfiguration configuration;

    /**
     * Constructs {@link FamilyValidator} with repository, filestore, master data, user client, and configuration service.
     *
     * @param familyRepository data repository for family lookups and exclusivity queries
     * @param fileStoreClient client verifying document existence in FileStore
     * @param familyMasterDataService service providing MDMS relationship master data
     * @param userClient client communicating with user service to verify citizen identity
     * @param familyConfigurationService service validating state family configuration policy
     * @param configuration family module configuration for verification rules
     */
    @Autowired
    public FamilyValidator(FamilyRepository familyRepository,
                           FileStoreClient fileStoreClient,
                           FamilyMasterDataService familyMasterDataService,
                           UserClient userClient,
                           @Autowired(required = false) FamilyConfigurationService familyConfigurationService,
                           @Autowired(required = false) org.upyog.family.config.FamilyConfiguration configuration) {
        this.familyRepository = familyRepository;
        this.fileStoreClient = fileStoreClient;
        this.familyMasterDataService = familyMasterDataService;
        this.userClient = userClient;
        this.familyConfigurationService = familyConfigurationService;
        this.configuration = configuration;
    }

    /**
     * Backward-compatible constructor for testing without FamilyConfiguration.
     */
    public FamilyValidator(FamilyRepository familyRepository,
                           FileStoreClient fileStoreClient,
                           FamilyMasterDataService familyMasterDataService,
                           UserClient userClient,
                           FamilyConfigurationService familyConfigurationService) {
        this(familyRepository, fileStoreClient, familyMasterDataService, userClient, familyConfigurationService, null);
    }

    /**
     * Backward-compatible constructor for testing without FamilyConfigurationService.
     */
    public FamilyValidator(FamilyRepository familyRepository,
                           FileStoreClient fileStoreClient,
                           FamilyMasterDataService familyMasterDataService,
                           UserClient userClient) {
        this(familyRepository, fileStoreClient, familyMasterDataService, userClient, null, null);
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
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_FAMILY_BODY_NULL);
        }

        // 0. Ensure caller is identity-verified in user registry database
        userClient.validateCallerIsVerified(request.getRequestInfo());

        Family family = request.getFamily();

        if (StringUtils.isBlank(family.getTenantId())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_FAMILY_TENANT_REQUIRED);
        }

        // Validate state family configuration policy
        if (familyConfigurationService != null) {
            familyConfigurationService.validateUpyogNativeFamily(request.getRequestInfo(), family.getTenantId());
        }

        // Validate family category against MDMS if supplied
        if (StringUtils.isNotBlank(family.getCategory())) {
            familyMasterDataService.validateCategory(request.getRequestInfo(), family.getTenantId(), family.getCategory());
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
            throw new CustomException(FamilyConstants.ERROR_HEAD_UUID_MISMATCH, FamilyConstants.MSG_HEAD_UUID_MISMATCH);
        }

        // Validate HoF relationship
        String headRelationship = headMember.getRelationshipWithHead();
        if (StringUtils.isNotBlank(headRelationship)) {
            String normalizedHeadRel = headRelationship.trim().toUpperCase();
            if (!FamilyConstants.RELATIONSHIP_SELF.equals(normalizedHeadRel) && !FamilyConstants.RELATIONSHIP_HEAD.equals(normalizedHeadRel)) {
                log.warn("[UPYOG-FAMILY-VALIDATOR] Head of Family relationship should be 'SELF', got: {}", headRelationship);
            }
        }

        // 2. Validate all members: uniqueness, MDMS taxonomy, Aadhaar/Identity verification, multi-family exclusivity, and assets
        Set<String> memberUuids = new HashSet<>();
        Set<String> declaredPropertyIds = new HashSet<>();

        for (FamilyMember member : family.getMembers()) {
            if (StringUtils.isBlank(member.getUserUuid())) {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_MEMBER_USER_UUID_REQUIRED);
            }
            if (!memberUuids.add(member.getUserUuid())) {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_DUPLICATE_MEMBER_UUID_PREFIX + member.getUserUuid());
            }

            // Fix for PR Comment #14 (FamilyValidator.java:180):
            // Minors and dependents lacking independent Aadhaar/e-KYC profiles are exempt from mandatory
            // individual identity verification when family.member.verification.mandatory.for.all is false,
            // provided the Head of Family and applicant caller are verified.
            boolean isHoF = Boolean.TRUE.equals(member.getIsHead());
            boolean requireVerification = isHoF
                    || (configuration != null && Boolean.TRUE.equals(configuration.getMemberVerificationMandatoryForAll()))
                    || !isMinorOrDependent(member);

            if (requireVerification) {
                userClient.validateUserIsVerified(member.getUserUuid(), request.getRequestInfo());
            } else {
                log.info("[UPYOG-FAMILY-VALIDATOR] Member {} is a minor/dependent under HoF guardianship; individual e-KYC verification exempt.",
                        member.getUserUuid());
            }

            // Dynamic MDMS validation for relationshipWithHead
            if (StringUtils.isNotBlank(member.getRelationshipWithHead())) {
                familyMasterDataService.validateRelationship(request.getRequestInfo(), family.getTenantId(), member.getRelationshipWithHead());
            }

            // Multi-family Exclusivity rule: Member cannot belong to another active family
            List<String> activeFamilyIds = familyRepository.findActiveFamilyIdsByMemberUuid(member.getUserUuid());
            if (activeFamilyIds != null && !activeFamilyIds.isEmpty()) {
                log.error("[UPYOG-FAMILY-VALIDATOR] Citizen {} already exists in active family: {}", member.getUserUuid(), activeFamilyIds);
                throw new CustomException(FamilyConstants.ERROR_MEMBER_ALREADY_EXISTS,
                        FamilyConstants.MSG_CITIZEN_ALREADY_ACTIVE_PREFIX + member.getUserUuid() + FamilyConstants.MSG_CITIZEN_ALREADY_ACTIVE_SUFFIX + activeFamilyIds.get(0));
            }

            // Validate citizen details and demographics
            validateCitizenDetails(member, request, family.getTenantId());

            // Validate declared properties for member
            validateMemberProperties(member, family.getTenantId(), declaredPropertyIds);

            // Validate declared income for member
            validateMemberIncome(member, request, family.getTenantId());

            // Validate declared vehicles for member
            validateMemberVehicles(member, family.getTenantId());
        }

        // 3. Validate residence property linkage
        // Fix for PR Comment #15 (FamilyValidator.java:210):
        // Validate residence property linkage against declared member properties.
        // If members have declared properties, ensure the family residence property
        // matches one of the declared property or survey identifiers, avoiding misleading no-op log.
        if (StringUtils.isNotBlank(family.getFamilyAddressPropertyId())) {
            if (!declaredPropertyIds.isEmpty() && !declaredPropertyIds.contains(family.getFamilyAddressPropertyId())) {
                log.error("[UPYOG-FAMILY-VALIDATOR] Family address property ID '{}' does not match any declared member property: {}",
                        family.getFamilyAddressPropertyId(), declaredPropertyIds);
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                        FamilyConstants.MSG_INVALID_RESIDENCE_PROPERTY_PREFIX + family.getFamilyAddressPropertyId()
                                + FamilyConstants.MSG_INVALID_RESIDENCE_PROPERTY_SUFFIX);
            }
            log.info("[UPYOG-FAMILY-VALIDATOR] Validated family residence property linkage: {}", family.getFamilyAddressPropertyId());
        }

        // 4. Validate family-level documents
        if (family.getDocuments() != null) {
            for (FamilyDocument doc : family.getDocuments()) {
                if (StringUtils.isBlank(doc.getFilestoreId())) {
                    throw new CustomException(FamilyConstants.ERROR_FILESTORE_NOT_FOUND, FamilyConstants.MSG_DOCUMENT_FILESTORE_ID_MANDATORY);
                }
                boolean exists = fileStoreClient.validateFileExists(family.getTenantId(), doc.getFilestoreId());
                if (!exists) {
                    throw new CustomException(FamilyConstants.ERROR_FILESTORE_NOT_FOUND,
                            FamilyConstants.MSG_FILESTORE_NOT_FOUND_PREFIX + doc.getFilestoreId());
                }
            }
        }
    }

    /**
     * Validates citizen demographic profile attributes and verifies attached identification proofs.
     *
     * @param member family member containing citizen demographics
     * @param request UPYOG family request envelope
     * @param tenantId common tenant identifier for master data and filestore resolution
     * @throws CustomException if social category or attached proofs are invalid
     */
    private void validateCitizenDetails(FamilyMember member, FamilyRequest request, String tenantId) {
        if (member.getCitizenDetails() == null) {
            return;
        }
        CitizenProfile profile = member.getCitizenDetails();
        if (StringUtils.isNotBlank(profile.getCaste())) {
            familyMasterDataService.validateCategory(request.getRequestInfo(), tenantId, profile.getCaste());
        }
        if (profile.getDocuments() != null) {
            for (UserDocument doc : profile.getDocuments()) {
                validateUserDocument(tenantId, doc);
            }
        }
    }

    /**
     * Validates property asset declarations for rural and urban landholdings.
     *
     * <p>Enforces:</p>
     * <ul>
     *   <li>Mandatory locality type ({@code URBAN} or {@code RURAL}).</li>
     *   <li>Presence of Gram Panchayat and village name for rural properties.</li>
     *   <li>Presence of valid UPYOG Property Tax assessment ID for urban properties.</li>
     *   <li>FileStore accessibility for attached property ownership documents.</li>
     * </ul>
     *
     * @param member family member declaring properties
     * @param tenantId common tenant identifier
     * @param declaredPropertyIds set tracking declared property and survey identifiers for residence linkage
     * @throws CustomException if mandatory property attributes or proof documents are missing or invalid
     */
    private void validateMemberProperties(FamilyMember member, String tenantId, Set<String> declaredPropertyIds) {
        if (member.getProperties() == null || member.getProperties().isEmpty()) {
            return;
        }
        for (UserProperty prop : member.getProperties()) {
            if (StringUtils.isBlank(prop.getLocalityType())) {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_LOCALITY_TYPE_MANDATORY);
            }
            String locality = prop.getLocalityType().trim().toUpperCase();
            if (FamilyConstants.LOCALITY_TYPE_RURAL.equals(locality)) {
                if (prop.getRuralPropertyDetail() == null) {
                    throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_RURAL_PROPERTY_DETAIL_REQUIRED);
                }
                RuralPropertyDetail rural = prop.getRuralPropertyDetail();
                if (StringUtils.isBlank(rural.getGramPanchayat()) || StringUtils.isBlank(rural.getVillageName())) {
                    throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_RURAL_GRAM_PANCHAYAT_VILLAGE_MANDATORY);
                }
                if (StringUtils.isNotBlank(rural.getSurveyNumber())) {
                    declaredPropertyIds.add(rural.getSurveyNumber());
                }
                if (StringUtils.isNotBlank(rural.getPropertyId())) {
                    declaredPropertyIds.add(rural.getPropertyId());
                }
            } else if (FamilyConstants.LOCALITY_TYPE_URBAN.equals(locality)) {
                if (prop.getUrbanPropertyDetail() == null) {
                    throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_URBAN_PROPERTY_DETAIL_REQUIRED);
                }
                UrbanPropertyDetail urban = prop.getUrbanPropertyDetail();
                if (StringUtils.isBlank(urban.getUpyogPropertyId())) {
                    throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_UPYOG_PROPERTY_ID_MANDATORY);
                }
                declaredPropertyIds.add(urban.getUpyogPropertyId());
            } else {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_INVALID_LOCALITY_TYPE_PREFIX + prop.getLocalityType() + FamilyConstants.MSG_INVALID_LOCALITY_TYPE_SUFFIX);
            }

            if (StringUtils.isNotBlank(prop.getPropertyId())) {
                declaredPropertyIds.add(prop.getPropertyId());
            }
            if (StringUtils.isNotBlank(prop.getId())) {
                declaredPropertyIds.add(prop.getId());
            }

            if (prop.getDocument() != null) {
                validateUserDocument(tenantId, prop.getDocument());
            }
        }
    }

    /**
     * Validates member income and employment declarations against UPYOG MDMS taxonomy.
     *
     * @param member family member declaring income
     * @param request UPYOG request context envelope
     * @param tenantId common tenant identifier
     * @throws CustomException if occupation is invalid in MDMS or attached proofs do not exist
     */
    private void validateMemberIncome(FamilyMember member, FamilyRequest request, String tenantId) {
        if (member.getIncome() == null) {
            return;
        }
        UserIncomeDetail income = member.getIncome();
        if (StringUtils.isNotBlank(income.getOccupation())) {
            familyMasterDataService.validateOccupation(request.getRequestInfo(), tenantId, income.getOccupation());
        }
        if (income.getSalaryProofDocument() != null) {
            validateUserDocument(tenantId, income.getSalaryProofDocument());
        }
        if (income.getTaxProofDocument() != null) {
            validateUserDocument(tenantId, income.getTaxProofDocument());
        }
    }

    /**
     * Validates declared vehicle ownership and verifies Registration Certificate (RC) proofs.
     *
     * @param member family member declaring vehicles
     * @param tenantId common tenant identifier
     * @throws CustomException if vehicle number is missing or RC document does not exist in FileStore
     */
    private void validateMemberVehicles(FamilyMember member, String tenantId) {
        if (member.getVehicles() == null || member.getVehicles().isEmpty()) {
            return;
        }
        for (UserVehicleDetail vehicle : member.getVehicles()) {
            if (StringUtils.isBlank(vehicle.getVehicleNumber())) {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_VEHICLE_NUMBER_MANDATORY);
            }
            if (vehicle.getRcDocument() != null) {
                validateUserDocument(tenantId, vehicle.getRcDocument());
            }
        }
    }

    /**
     * Verifies that the specified user verification document exists and is accessible in UPYOG FileStore.
     *
     * @param tenantId tenant identifier
     * @param doc verification document containing FileStore ID
     * @throws CustomException if the document is missing a filestoreId or file is not found in FileStore
     */
    private void validateUserDocument(String tenantId, UserDocument doc) {
        if (doc != null && StringUtils.isNotBlank(doc.getFilestoreId())) {
            boolean exists = fileStoreClient.validateFileExists(tenantId, doc.getFilestoreId());
            if (!exists) {
                throw new CustomException(FamilyConstants.ERROR_FILESTORE_NOT_FOUND,
                        FamilyConstants.MSG_FILESTORE_NOT_FOUND_PREFIX + doc.getFilestoreId());
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
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, FamilyConstants.MSG_TARGET_FAMILY_NOT_EXISTS);
        }

        // Ensure caller is identity-verified in user registry database
        userClient.validateCallerIsVerified(request.getRequestInfo());

        if (!FamilyStatus.ACTIVE.equals(existingFamily.getStatus())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_CANNOT_UPDATE_FAMILY_STATUS_PREFIX + existingFamily.getStatus());
        }

        // Validate state family configuration policy
        if (familyConfigurationService != null) {
            familyConfigurationService.validateUpyogNativeFamily(request.getRequestInfo(), existingFamily.getTenantId());
        }

        // Caller authorization: Only current HoF is authorized to update family metadata
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null) {
            String callerUuid = request.getRequestInfo().getUserInfo().getUuid();
            if (StringUtils.isNotBlank(callerUuid) && !FamilyConstants.SYSTEM_USER.equalsIgnoreCase(callerUuid)) {
                if (!callerUuid.equals(existingFamily.getHeadUserUuid())) {
                    throw new CustomException(FamilyConstants.ERROR_UNAUTHORIZED_ACCESS,
                            FamilyConstants.MSG_ONLY_HOF_AUTHORIZED_UPDATE);
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
                // Fix for PR Comment #12 (FamilyServiceImpl.java:394):
                // HoF relationship was renamed from 'HEAD' to 'SELF' to match active MDMS taxonomy.
                // Normalize legacy 'HEAD' to 'SELF' for HoF before MDMS validation so existing records/payloads do not fail.
                if (Boolean.TRUE.equals(member.getIsHead()) && FamilyConstants.RELATIONSHIP_HEAD.equalsIgnoreCase(member.getRelationshipWithHead())) {
                    log.info("[UPYOG-FAMILY-VALIDATOR] Normalizing legacy HoF relationship 'HEAD' to 'SELF' on update for userUuid: {}",
                            member.getUserUuid());
                    member.setRelationshipWithHead(FamilyConstants.RELATIONSHIP_SELF);
                }

                // Dynamic MDMS validation for relationshipWithHead
                if (StringUtils.isNotBlank(member.getRelationshipWithHead())) {
                    familyMasterDataService.validateRelationship(request.getRequestInfo(), existingFamily.getTenantId(), member.getRelationshipWithHead());
                }

                // Verify member is not already an active member in another active family
                if (Boolean.TRUE.equals(member.getActive()) || member.getActive() == null) {
                    List<String> activeFamilyIds = familyRepository.findActiveFamilyIdsByMemberUuid(member.getUserUuid());
                    if (activeFamilyIds != null && !activeFamilyIds.isEmpty() && !activeFamilyIds.contains(existingFamily.getFamilyId())) {
                        throw new CustomException(FamilyConstants.ERROR_MEMBER_ALREADY_EXISTS,
                                FamilyConstants.MSG_CITIZEN_ALREADY_ACTIVE_PREFIX + member.getUserUuid() + FamilyConstants.MSG_CITIZEN_ALREADY_ACTIVE_SUFFIX + activeFamilyIds.get(0));
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
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, FamilyConstants.MSG_PARENT_FAMILY_NOT_FOUND_PREFIX + request.getParentFamilyId());
        }

        // Validate state family configuration policy
        if (familyConfigurationService != null) {
            familyConfigurationService.validateUpyogNativeFamily(request.getRequestInfo(), request.getTenantId());
        }

        // Ensure caller is identity-verified in user registry database
        userClient.validateCallerIsVerified(request.getRequestInfo());
        // Ensure new family Head of Family is identity-verified in user registry database
        userClient.validateUserIsVerified(request.getNewFamilyHeadUserUuid(), request.getRequestInfo());

        if (!FamilyStatus.ACTIVE.equals(parentFamily.getStatus())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_SPLIT, FamilyConstants.MSG_PARENT_FAMILY_NOT_ACTIVE);
        }

        // Caller authorization: Only parent family HoF can initiate split
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null) {
            String callerUuid = request.getRequestInfo().getUserInfo().getUuid();
            if (StringUtils.isNotBlank(callerUuid) && !FamilyConstants.SYSTEM_USER.equalsIgnoreCase(callerUuid)) {
                if (!callerUuid.equals(parentFamily.getHeadUserUuid())) {
                    throw new CustomException(FamilyConstants.ERROR_UNAUTHORIZED_ACCESS,
                            FamilyConstants.MSG_ONLY_PARENT_HOF_AUTHORIZED_SPLIT);
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
                        FamilyConstants.MSG_SPLITTING_MEMBER_NOT_ACTIVE_PREFIX + splittingUuid + FamilyConstants.MSG_SPLITTING_MEMBER_NOT_ACTIVE_SUFFIX);
            }
        }

        if (!request.getSplittingMemberUserUuids().contains(request.getNewFamilyHeadUserUuid())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_SPLIT,
                    FamilyConstants.MSG_NEW_HOF_MUST_BE_SPLITTING_PREFIX + request.getNewFamilyHeadUserUuid() + FamilyConstants.MSG_NEW_HOF_MUST_BE_SPLITTING_SUFFIX);
        }

        if (request.getSplittingMemberUserUuids().size() >= parentMemberUuids.size()) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_SPLIT,
                    FamilyConstants.MSG_CANNOT_SPLIT_ALL_MEMBERS);
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
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, FamilyConstants.MSG_SURVIVING_FAMILY_NOT_FOUND_PREFIX + request.getSurvivingFamilyId());
        }
        if (mergingFamily == null) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, FamilyConstants.MSG_MERGING_FAMILY_NOT_FOUND_PREFIX + request.getMergingFamilyId());
        }

        // Validate state family configuration policy
        if (familyConfigurationService != null) {
            familyConfigurationService.validateUpyogNativeFamily(request.getRequestInfo(), request.getTenantId());
        }

        // Ensure caller is identity-verified in user registry database
        userClient.validateCallerIsVerified(request.getRequestInfo());
        // Ensure nominated common Head of Family is identity-verified in user registry database
        userClient.validateUserIsVerified(request.getCommonHeadUserUuid(), request.getRequestInfo());

        if (survivingFamily.getFamilyId().equals(mergingFamily.getFamilyId())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_MERGE, FamilyConstants.MSG_MERGE_FAMILY_INTO_ITSELF);
        }

        if (!FamilyStatus.ACTIVE.equals(survivingFamily.getStatus())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_MERGE, FamilyConstants.MSG_SURVIVING_FAMILY_NOT_ACTIVE);
        }
        if (!FamilyStatus.ACTIVE.equals(mergingFamily.getStatus())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_MERGE, FamilyConstants.MSG_MERGING_FAMILY_NOT_ACTIVE);
        }

        // Caller authorization: Caller must be HoF of one of the merging family units
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null) {
            String callerUuid = request.getRequestInfo().getUserInfo().getUuid();
            if (StringUtils.isNotBlank(callerUuid) && !FamilyConstants.SYSTEM_USER.equalsIgnoreCase(callerUuid)) {
                if (!callerUuid.equals(survivingFamily.getHeadUserUuid()) && !callerUuid.equals(mergingFamily.getHeadUserUuid())) {
                    throw new CustomException(FamilyConstants.ERROR_UNAUTHORIZED_ACCESS,
                            FamilyConstants.MSG_ONLY_HOF_AUTHORIZED_MERGE);
                }
            }
        }

        Set<String> allActiveMemberUuids = new HashSet<>();
        survivingFamily.getMembers().stream().filter(m -> Boolean.TRUE.equals(m.getActive())).forEach(m -> allActiveMemberUuids.add(m.getUserUuid()));
        mergingFamily.getMembers().stream().filter(m -> Boolean.TRUE.equals(m.getActive())).forEach(m -> allActiveMemberUuids.add(m.getUserUuid()));

        if (!allActiveMemberUuids.contains(request.getCommonHeadUserUuid())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_MERGE,
                    FamilyConstants.MSG_COMMON_HOF_MUST_BELONG_PREFIX + request.getCommonHeadUserUuid() + FamilyConstants.MSG_COMMON_HOF_MUST_BELONG_SUFFIX);
        }
    }

    /**
     * Fix for PR Comment #14 (FamilyValidator.java:180):
     * Check if a family member is a minor or dependent, qualifying for exemption
     * from mandatory individual Aadhaar/e-KYC verification under HoF guardianship.
     *
     * @param member family member to evaluate
     * @return true if member is a minor (< 18 yrs), child, dependent, or has HoF consent token
     */
    private boolean isMinorOrDependent(FamilyMember member) {
        if (member == null) {
            return false;
        }
        String rel = member.getRelationshipWithHead();
        if (StringUtils.isNotBlank(rel)) {
            String upperRel = rel.trim().toUpperCase();
            if (FamilyConstants.RELATIONSHIP_DEPENDENT.equals(upperRel)
                    || FamilyConstants.RELATIONSHIP_CHILD.equals(upperRel)) {
                return true;
            }
        }
        if (member.getDob() != null && member.getDob() > 0) {
            long ageInMillis = System.currentTimeMillis() - member.getDob();
            long eighteenYearsInMillis = 18L * 365L * 24L * 60L * 60L * 1000L;
            if (ageInMillis < eighteenYearsInMillis) {
                return true;
            }
        }
        return StringUtils.isNotBlank(member.getConsentToken());
    }
}
