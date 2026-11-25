package org.upyog.family.domain.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
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
import org.upyog.family.web.contract.AddMemberRequest;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.RuralPropertyDetail;
import org.upyog.family.web.contract.UrbanPropertyDetail;
import org.upyog.family.web.contract.UserDocument;
import org.upyog.family.web.contract.UserIncomeDetail;
import org.upyog.family.web.contract.UserProperty;
import org.upyog.family.web.contract.UserVehicleDetail;

import java.math.BigDecimal;
import java.util.List;

/**
 * Specialized validator validating business rules for adding a member to an existing Family Unit in UPYOG.
 *
 * <p>Enforces dynamic MDMS relationship verification, active family exclusivity, Aadhaar OTP consent,
 * and declared 3NF citizen profile attributes, properties, income, and vehicles.</p>
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
    private final UserClient userClient;
    private final FamilyConfigurationService familyConfigurationService;
    private final FileStoreClient fileStoreClient;

    /**
     * Constructs an AddMemberValidator with required repository, master data, DX requester, user client, configuration service, and filestore client.
     *
     * @param familyRepository repository for family persistence and query lookups
     * @param masterDataService service for MDMS relationship validation
     * @param requesterDxClient client for validating consent tokens against DX requester
     * @param userClient client for user profile lookups and identity verification
     * @param familyConfigurationService service validating state family configuration policy
     * @param fileStoreClient client for verifying document existence in FileStore
     */
    @Autowired
    public AddMemberValidator(FamilyRepository familyRepository,
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
    public AddMemberValidator(FamilyRepository familyRepository,
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
    public AddMemberValidator(FamilyRepository familyRepository,
                              FamilyMasterDataService masterDataService,
                              RequesterDxClient requesterDxClient,
                              UserClient userClient) {
        this(familyRepository, masterDataService, requesterDxClient, userClient, null, null);
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

        // Ensure caller is identity-verified in user registry database
        userClient.validateCallerIsVerified(request.getRequestInfo());

        // Validate state family configuration policy
        if (familyConfigurationService != null) {
            familyConfigurationService.validateUpyogNativeFamily(request.getRequestInfo(), request.getTenantId());
        }

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
        if (member == null) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Either member userUuid or mobileNumber is mandatory for adding a family member.");
        }

        // Bind resolved assets and demographics from request to member adhering to 3NF architecture
        if (member.getCitizenDetails() == null && request.getCitizenDetails() != null) {
            member.setCitizenDetails(request.getCitizenDetails());
        }
        if ((member.getProperties() == null || member.getProperties().isEmpty()) && !request.getProperties().isEmpty()) {
            member.setProperties(request.getProperties());
        }
        if (member.getIncome() == null && request.getIncome() != null) {
            member.setIncome(request.getIncome());
        }
        if ((member.getVehicles() == null || member.getVehicles().isEmpty()) && !request.getVehicles().isEmpty()) {
            member.setVehicles(request.getVehicles());
        }

        // Derive transient demographic attributes from citizenDetails if present
        if (member.getCitizenDetails() != null) {
            CitizenProfile profile = member.getCitizenDetails();
            if (StringUtils.isBlank(member.getName()) && StringUtils.isNotBlank(profile.getName())) {
                member.setName(profile.getName());
            }
            if (StringUtils.isBlank(member.getMobileNumber()) && StringUtils.isNotBlank(profile.getMobileNumber())) {
                member.setMobileNumber(profile.getMobileNumber());
            }
            if (StringUtils.isBlank(member.getGender()) && StringUtils.isNotBlank(profile.getGender())) {
                member.setGender(profile.getGender());
            }
            if (member.getDob() == null && profile.getDob() != null) {
                member.setDob(profile.getDob());
            }
            if (StringUtils.isBlank(member.getPhoto()) && StringUtils.isNotBlank(profile.getPhoto())) {
                member.setPhoto(profile.getPhoto());
            }
            if (StringUtils.isBlank(member.getUserUuid()) && StringUtils.isNotBlank(profile.getUuid())) {
                member.setUserUuid(profile.getUuid());
            }
        }

        if (StringUtils.isBlank(member.getUserUuid()) && StringUtils.isNotBlank(request.getUserUuid())) {
            member.setUserUuid(request.getUserUuid());
        }
        if (StringUtils.isBlank(member.getRelationshipWithHead()) && StringUtils.isNotBlank(request.getRelationshipWithHead())) {
            member.setRelationshipWithHead(request.getRelationshipWithHead());
        }

        if (StringUtils.isBlank(member.getUserUuid()) && StringUtils.isBlank(member.getMobileNumber())) {
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

        // 6. Validate citizen details and demographics
        validateCitizenDetails(member, request.getRequestInfo(), request.getTenantId());

        // 7. Validate declared properties for member
        validateMemberProperties(member, request.getRequestInfo(), request.getTenantId());

        // 8. Validate declared income for member
        validateMemberIncome(member, request.getRequestInfo(), request.getTenantId());

        // 9. Validate declared vehicles for member
        validateMemberVehicles(member, request.getTenantId());

        log.info("[UPYOG-ADD-MEMBER-VALIDATOR] Successfully validated add member request for citizen: {}", member.getUserUuid());
    }

    /**
     * Validates citizen demographic profile attributes and verifies attached identification proofs.
     *
     * @param member family member containing citizen demographics
     * @param requestInfo platform request metadata
     * @param tenantId common tenant identifier for master data and filestore resolution
     * @throws CustomException if social category or attached proofs are invalid
     */
    private void validateCitizenDetails(FamilyMember member, RequestInfo requestInfo, String tenantId) {
        if (member.getCitizenDetails() == null) {
            return;
        }
        CitizenProfile profile = member.getCitizenDetails();
        if (StringUtils.isNotBlank(profile.getCaste())) {
            masterDataService.validateCategory(requestInfo, tenantId, profile.getCaste());
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
     * @param member family member declaring properties
     * @param requestInfo platform request metadata
     * @param tenantId common tenant identifier
     * @throws CustomException if mandatory property attributes or proof documents are missing or invalid
     */
    private void validateMemberProperties(FamilyMember member, RequestInfo requestInfo, String tenantId) {
        List<UserProperty> properties = member.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        for (UserProperty prop : properties) {
            if (StringUtils.isBlank(prop.getLocalityType())) {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_LOCALITY_TYPE_MANDATORY);
            }
            masterDataService.validateOwnershipType(requestInfo, tenantId, prop.getOwnershipType());

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
                // Validate OTP ownership authorization consent token for declared urban property (mandatory for URBAN)
                String consentToken = StringUtils.isNotBlank(urban.getConsentToken()) ? urban.getConsentToken() : prop.getConsentToken();
                if (StringUtils.isBlank(consentToken)) {
                    throw new CustomException(FamilyConstants.ERROR_PROPERTY_CONSENT_TOKEN_REQUIRED,
                            FamilyConstants.MSG_PROPERTY_CONSENT_TOKEN_REQUIRED + urban.getUpyogPropertyId());
                }
                if (userClient == null) {
                    throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                            FamilyConstants.MSG_USER_CLIENT_UNAVAILABLE_FOR_CONSENT);
                }
                boolean valid = userClient.validatePropertyConsentToken(urban.getUpyogPropertyId(), consentToken, requestInfo);
                if (!valid) {
                    throw new CustomException(FamilyConstants.ERROR_INVALID_PROPERTY_CONSENT_TOKEN,
                            FamilyConstants.MSG_INVALID_PROPERTY_CONSENT_TOKEN_PREFIX + urban.getUpyogPropertyId());
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
     * @param member family member declaring income
     * @param requestInfo platform request metadata
     * @param tenantId common tenant identifier
     * @throws CustomException if occupation is invalid in MDMS or attached proofs do not exist
     */
    private void validateMemberIncome(FamilyMember member, RequestInfo requestInfo, String tenantId) {
        UserIncomeDetail income = member.getIncome();
        if (income == null) {
            return;
        }
        if (income.getAnnualIncome() != null && income.getAnnualIncome().compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "Annual income cannot be negative.");
        }
        if (StringUtils.isNotBlank(income.getOccupation())) {
            masterDataService.validateOccupation(requestInfo, tenantId, income.getOccupation());
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
        List<UserVehicleDetail> vehicles = member.getVehicles();
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
