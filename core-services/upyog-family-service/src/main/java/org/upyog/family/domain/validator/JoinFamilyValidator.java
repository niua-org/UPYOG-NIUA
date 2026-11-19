package org.upyog.family.domain.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.service.config.FamilyConfigurationService;
import org.upyog.family.domain.service.master.FamilyMasterDataService;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.FileStoreClient;
import org.upyog.family.persistence.repository.RequesterDxClient;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.JoinFamilyRequest;
import org.upyog.family.web.contract.RuralPropertyDetail;
import org.upyog.family.web.contract.UrbanPropertyDetail;
import org.upyog.family.web.contract.UserDocument;
import org.upyog.family.web.contract.UserIncomeDetail;
import org.upyog.family.web.contract.UserProperty;
import org.upyog.family.web.contract.UserVehicleDetail;

import java.math.BigDecimal;
import java.util.List;

/**
 * Specialized validator validating business rules for a citizen joining an existing Family Unit in UPYOG.
 *
 * <p>Validates applicant availability, dynamic MDMS relationship, HoF approval consent,
 * and declared 3NF citizen profile attributes, properties, income, and vehicles.</p>
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
    private final UserClient userClient;
    private final FamilyConfigurationService familyConfigurationService;
    private final FileStoreClient fileStoreClient;

    /**
     * Constructs a JoinFamilyValidator with required repository, master data, DX requester, user client, configuration service, and filestore client.
     *
     * @param familyRepository repository for family persistence and query lookups
     * @param masterDataService service for MDMS relationship validation
     * @param requesterDxClient client for validating consent tokens against DX requester
     * @param userClient client for user profile lookups and identity verification
     * @param familyConfigurationService service validating state family configuration policy
     * @param fileStoreClient client for verifying document existence in FileStore
     */
    @Autowired
    public JoinFamilyValidator(FamilyRepository familyRepository,
                               FamilyMasterDataService masterDataService,
                               RequesterDxClient requesterDxClient,
                               UserClient userClient,
                               @Autowired(required = false) FamilyConfigurationService familyConfigurationService,
                               @Autowired(required = false) FileStoreClient fileStoreClient) {
        this.familyRepository = familyRepository;
        this.masterDataService = masterDataService;
        this.requesterDxClient = requesterDxClient;
        this.userClient = userClient;
        this.familyConfigurationService = familyConfigurationService;
        this.fileStoreClient = fileStoreClient;
    }

    /**
     * Backward-compatible constructor without filestore client.
     *
     * @param familyRepository repository for family persistence and query lookups
     * @param masterDataService service for MDMS relationship validation
     * @param requesterDxClient client for validating consent tokens against DX requester
     * @param userClient client for user profile lookups and identity verification
     * @param familyConfigurationService service validating state family configuration policy
     */
    public JoinFamilyValidator(FamilyRepository familyRepository,
                               FamilyMasterDataService masterDataService,
                               RequesterDxClient requesterDxClient,
                               UserClient userClient,
                               FamilyConfigurationService familyConfigurationService) {
        this(familyRepository, masterDataService, requesterDxClient, userClient, familyConfigurationService, null);
    }

    /**
     * Backward-compatible constructor without dynamic family configuration service or filestore client.
     *
     * @param familyRepository repository for family persistence and query lookups
     * @param masterDataService service for MDMS relationship validation
     * @param requesterDxClient client for validating consent tokens against DX requester
     * @param userClient client for user profile lookups and identity verification
     */
    public JoinFamilyValidator(FamilyRepository familyRepository,
                               FamilyMasterDataService masterDataService,
                               RequesterDxClient requesterDxClient,
                               UserClient userClient) {
        this(familyRepository, masterDataService, requesterDxClient, userClient, null, null);
    }

    /**
     * Validates an independent citizen request to join an existing Family Unit in UPYOG.
     * Verifies Head of Family authorization consent, ensures applicant is not already active in another family,
     * checks relationship against MDMS master data, and validates declared assets and documents.
     *
     * @param request join family request containing applicant UUID, HoF consent token, and asset declarations
     * @param existingFamily target Family aggregate
     * @throws CustomException if authorization, exclusivity, or asset constraints fail
     */
    public void validate(JoinFamilyRequest request, Family existingFamily) {
        log.info("[UPYOG-JOIN-FAMILY-VALIDATOR] Validating join family request: applicant={}, familyId={}",
                request != null ? request.getApplicantUserUuid() : null,
                request != null ? request.getFamilyId() : null);

        if (request == null) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "JoinFamilyRequest cannot be null.");
        }

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

        // Ensure caller is identity-verified in user registry database
        userClient.validateCallerIsVerified(request.getRequestInfo());

        // Validate state family configuration policy
        if (familyConfigurationService != null) {
            familyConfigurationService.validateUpyogNativeFamily(request.getRequestInfo(), request.getTenantId());
        }

        // Caller authorization: Authenticated caller details in RequestInfo.userInfo are mandatory to prevent impersonation
        if (request.getRequestInfo() == null || request.getRequestInfo().getUserInfo() == null
                || StringUtils.isBlank(request.getRequestInfo().getUserInfo().getUuid())) {
            throw new CustomException(FamilyConstants.ERROR_UNAUTHORIZED_ACCESS,
                    "Authenticated caller details in RequestInfo.userInfo are mandatory.");
        }
        String callerUuid = request.getRequestInfo().getUserInfo().getUuid();
        if (!FamilyConstants.SYSTEM_USER.equalsIgnoreCase(callerUuid)
                && !callerUuid.equals(request.getApplicantUserUuid())) {
            throw new CustomException(FamilyConstants.ERROR_UNAUTHORIZED_ACCESS,
                    "Applicant userUuid must match the authenticated caller.");
        }

        // Ensure applicant is identity-verified in user registry database
        userClient.validateUserIsVerified(request.getApplicantUserUuid(), request.getRequestInfo());

        // Validate relationship against UPYOG MDMS
        masterDataService.validateRelationship(request.getRequestInfo(), request.getTenantId(), request.getRelationshipWithHead());

        // Check if applicant already belongs to an active family
        List<String> activeFamilyIds = familyRepository.findActiveFamilyIdsByMemberUuid(request.getApplicantUserUuid());
        if (!activeFamilyIds.isEmpty()) {
            throw new CustomException(FamilyConstants.ERROR_MEMBER_ALREADY_EXISTS,
                    "Applicant already belongs to active family: " + activeFamilyIds.get(0));
        }

        // Validate HoF authorization transaction identifier (txnId)
        if (StringUtils.isBlank(request.getTxnId())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Head of Family authorization transaction identifier (txnId) is mandatory to join this family.");
        }

        // ========================================================================================================
        // 3NF Member Asset & Demographics Validations (Added for Parity with Family Creation / FamilyValidator)
        // ========================================================================================================
        // When joining a family, a citizen can declare their profile attributes, properties, income, and vehicles.
        // These validations guarantee that:
        // 1. Social caste and occupation adhere strictly to state MDMS masters (zero hardcoding).
        // 2. Rural properties have valid Gram Panchayat & village designations.
        // 3. Urban properties reference valid UPYOG Property Tax assessment IDs (upyogPropertyId).
        // 4. All attached document proofs (deeds, RC, salary/tax slips) exist in FileStore before persistence.
        // If the citizen has no assets to declare, each check safely skips without throwing false errors.

        // Validate citizen details and demographics (caste MDMS & identity proof documents)
        validateCitizenDetails(request, request.getTenantId());

        // Validate declared properties (rural landholdings vs urban municipal properties)
        validateProperties(request, request.getTenantId());

        // Validate declared income (occupation MDMS & salary/tax documents)
        validateIncome(request, request.getTenantId());

        // Validate declared vehicles (mandatory vehicleNumber & RC document)
        validateVehicles(request, request.getTenantId());

        log.info("[UPYOG-JOIN-FAMILY-VALIDATOR] Successfully validated join family request for applicant: {}", request.getApplicantUserUuid());
    }

    /**
     * Validates citizen demographic profile attributes and verifies attached identification proofs.
     *
     * @param request join family request
     * @param tenantId common tenant identifier
     * @throws CustomException if social category or attached proofs are invalid
     */
    private void validateCitizenDetails(JoinFamilyRequest request, String tenantId) {
        CitizenProfile profile = request.getCitizenDetails();
        if (profile == null) {
            return;
        }
        if (StringUtils.isNotBlank(profile.getCaste())) {
            masterDataService.validateCategory(request.getRequestInfo(), tenantId, profile.getCaste());
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
     * @param request join family request
     * @param tenantId common tenant identifier
     * @throws CustomException if mandatory property attributes or proof documents are missing or invalid
     */
    private void validateProperties(JoinFamilyRequest request, String tenantId) {
        List<UserProperty> properties = request.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        for (UserProperty prop : properties) {
            if (StringUtils.isBlank(prop.getLocalityType())) {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_LOCALITY_TYPE_MANDATORY);
            }
            masterDataService.validateOwnershipType(request.getRequestInfo(), tenantId, prop.getOwnershipType());
            String locality = prop.getLocalityType().trim().toUpperCase();
            if (FamilyConstants.LOCALITY_TYPE_RURAL.equals(locality)) {
                if (prop.getRuralPropertyDetail() == null) {
                    throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_RURAL_PROPERTY_DETAIL_REQUIRED);
                }
                RuralPropertyDetail rural = prop.getRuralPropertyDetail();
                if (StringUtils.isBlank(rural.getGramPanchayat()) || StringUtils.isBlank(rural.getVillageName())) {
                    throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_RURAL_GRAM_PANCHAYAT_VILLAGE_MANDATORY);
                }
            } else if (FamilyConstants.LOCALITY_TYPE_URBAN.equals(locality)) {
                if (prop.getUrbanPropertyDetail() == null) {
                    throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_URBAN_PROPERTY_DETAIL_REQUIRED);
                }
                UrbanPropertyDetail urban = prop.getUrbanPropertyDetail();
                if (StringUtils.isBlank(urban.getUpyogPropertyId())) {
                    throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_UPYOG_PROPERTY_ID_MANDATORY);
                }
            } else {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                        FamilyConstants.MSG_INVALID_LOCALITY_TYPE_PREFIX + prop.getLocalityType() + FamilyConstants.MSG_INVALID_LOCALITY_TYPE_SUFFIX);
            }

            if (prop.getDocument() != null) {
                validateUserDocument(tenantId, prop.getDocument());
            }
        }
    }

    /**
     * Validates member income and employment declarations against UPYOG MDMS taxonomy.
     *
     * @param request join family request
     * @param tenantId common tenant identifier
     * @throws CustomException if occupation is invalid in MDMS or attached proofs do not exist
     */
    private void validateIncome(JoinFamilyRequest request, String tenantId) {
        UserIncomeDetail income = request.getIncome();
        if (income == null) {
            return;
        }
        if (income.getAnnualIncome() != null && income.getAnnualIncome().compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "Annual income cannot be negative.");
        }
        if (StringUtils.isNotBlank(income.getOccupation())) {
            masterDataService.validateOccupation(request.getRequestInfo(), tenantId, income.getOccupation());
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
     * @param request join family request
     * @param tenantId common tenant identifier
     * @throws CustomException if vehicle number is missing or RC document does not exist in FileStore
     */
    private void validateVehicles(JoinFamilyRequest request, String tenantId) {
        List<UserVehicleDetail> vehicles = request.getVehicles();
        if (vehicles == null || vehicles.isEmpty()) {
            return;
        }
        for (UserVehicleDetail vehicle : vehicles) {
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
        if (doc != null) {
            if (StringUtils.isBlank(doc.getFilestoreId())) {
                throw new CustomException(FamilyConstants.ERROR_FILESTORE_NOT_FOUND,
                        FamilyConstants.MSG_DOCUMENT_FILESTORE_ID_MANDATORY);
            }
            if (fileStoreClient != null) {
                boolean exists = fileStoreClient.validateFileExists(tenantId, doc.getFilestoreId());
                if (!exists) {
                    throw new CustomException(FamilyConstants.ERROR_FILESTORE_NOT_FOUND,
                            FamilyConstants.MSG_FILESTORE_NOT_FOUND_PREFIX + doc.getFilestoreId());
                }
            }
        }
    }
}
