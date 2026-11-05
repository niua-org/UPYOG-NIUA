package org.egov.user.domain.service.profile;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.egov.tracer.model.Error;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.AddressType;
import org.egov.user.domain.model.enums.BloodGroup;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.model.enums.GuardianRelation;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.service.address.AddressMasterDataService;
import org.egov.user.domain.service.address.AddressService;
import org.egov.user.domain.service.document.UserDocumentService;
import org.egov.user.domain.service.identity.IdentityVerificationService;
import org.egov.user.domain.service.identity.VerifiedAadhaarDemographics;
import org.egov.user.domain.service.profile.validator.ProfileValidator;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.util.MaskUtils;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.FamilyRestRepository;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.UserRequest;
import org.egov.user.web.contract.UserSearchResponseContent;
import org.egov.user.web.contract.document.UserDocumentSearchCriteria;
import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.profile.AadhaarCitizenVerifyRequest;
import org.egov.user.web.contract.profile.AadhaarCitizenVerifyResponse;
import org.egov.user.web.contract.profile.ProfileCreateRequest;
import org.egov.user.web.contract.profile.ProfileIncludeEntity;
import org.egov.user.web.contract.profile.ProfileInitRequest;
import org.egov.user.web.contract.profile.ProfileResyncRequest;
import org.egov.user.web.contract.profile.ProfileSearchRequest;
import org.egov.user.web.contract.profile.ProfileUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
/**
 * Core implementation of {@link ProfileService} orchestrating citizen profile onboarding,
 * demographic updates with source-based field protection, and real-time Aadhaar profile re-sync.
 *
 * <p>Strict adherence to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Exclusively coordinates the profile lifecycle
 *       operations (create, init, update, search, and resync). Demographic validations are delegated to
 *       {@link ProfileValidator}, external Aadhaar OTP verification to {@link IdentityVerificationService},
 *       and persistence to {@link UserRepository}, {@link AddressRepository}, and {@link UserDocumentRepository}.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> Extensible to new document categories, authentication schemes,
 *       and demographic verification adapters without altering core profile orchestration pipelines.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Relies purely on injected Spring repository and service abstractions.</li>
 * </ul>
 *
 * <p>Key business rules enforced:</p>
 * <ul>
 *   <li>Section 8 Mandatory fields required on creation: Name, Father's Name, Mother's Name, Mobile Number,
 *       DOB, Email ID, Gender, Address (City, Locality, Street, House Number, Landmark, Pincode, Type),
 *       and Document Proofs (Address Proof, Identity Proof, DOB Proof).</li>
 *   <li>Section 8 Non-editable fields locked during updates unless authenticated via Aadhaar re-sync:
 *       Mobile Number, Father's Name, Mother's Name, Email ID are permanently locked; Name, DOB, Gender
 *       can only be updated via verified Aadhaar e-KYC re-sync.</li>
 *   <li>Safe PII encryption preventing duplicate encryption of already-encrypted tokens.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final ProfileValidator profileValidator;
    private final IdentityVerificationService identityVerificationService;
    private final EncryptionDecryptionUtil encryptionDecryptionUtil;
    private final AddressRepository addressRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final AddressMasterDataService addressMasterDataService;
    private final AddressService addressService;
    private final UserDocumentService userDocumentService;
    private final FamilyRestRepository familyRestRepository;
    @Autowired(required = false)
    @Setter
    private UserConfiguration userConfiguration;

    /**
     * Primary Spring-managed constructor injecting core repositories, validators, modular address/document services,
     * and optional family REST repository for SSOT familyId enrichment.
     *
     * @param userRepository              repository for user persistence
     * @param profileValidator            validator for profile request lifecycle rules
     * @param identityVerificationService adapter service for UIDAI Aadhaar verification
     * @param encryptionDecryptionUtil    utility for PII encryption/decryption
     * @param addressRepository           repository for address persistence
     * @param userDocumentRepository      repository for document proof persistence
     * @param addressMasterDataService    service for MDMS address boundary resolution
     * @param addressService              service for address lifecycle management
     * @param userDocumentService         service for document proof lifecycle management
     * @param familyRestRepository        repository for family service lookups
     */
    @Autowired
    public ProfileServiceImpl(UserRepository userRepository,
                              ProfileValidator profileValidator,
                              IdentityVerificationService identityVerificationService,
                              EncryptionDecryptionUtil encryptionDecryptionUtil,
                              AddressRepository addressRepository,
                              UserDocumentRepository userDocumentRepository,
                              @Autowired(required = false) AddressMasterDataService addressMasterDataService,
                              @Autowired(required = false) AddressService addressService,
                              @Autowired(required = false) UserDocumentService userDocumentService,
                              @Autowired(required = false) FamilyRestRepository familyRestRepository) {
        this(userRepository, profileValidator, identityVerificationService, encryptionDecryptionUtil, addressRepository, userDocumentRepository, addressMasterDataService, addressService, userDocumentService, null, familyRestRepository);
    }

    /**
     * Constructor for backward compatibility with 9 arguments.
     */
    public ProfileServiceImpl(UserRepository userRepository,
                              ProfileValidator profileValidator,
                              IdentityVerificationService identityVerificationService,
                              EncryptionDecryptionUtil encryptionDecryptionUtil,
                              AddressRepository addressRepository,
                              UserDocumentRepository userDocumentRepository,
                              AddressMasterDataService addressMasterDataService,
                              AddressService addressService,
                              UserDocumentService userDocumentService) {
        this(userRepository, profileValidator, identityVerificationService, encryptionDecryptionUtil, addressRepository, userDocumentRepository, addressMasterDataService, addressService, userDocumentService, null, null);
    }

    /**
     * Constructor including optional userConfiguration for configurable state tenant resolution.
     */
    public ProfileServiceImpl(UserRepository userRepository,
                              ProfileValidator profileValidator,
                              IdentityVerificationService identityVerificationService,
                              EncryptionDecryptionUtil encryptionDecryptionUtil,
                              AddressRepository addressRepository,
                              UserDocumentRepository userDocumentRepository,
                              AddressMasterDataService addressMasterDataService,
                              AddressService addressService,
                              UserDocumentService userDocumentService,
                              UserConfiguration userConfiguration) {
        this(userRepository, profileValidator, identityVerificationService, encryptionDecryptionUtil, addressRepository, userDocumentRepository, addressMasterDataService, addressService, userDocumentService, userConfiguration, null);
    }

    /**
     * Full constructor including all dependencies.
     */
    public ProfileServiceImpl(UserRepository userRepository,
                              ProfileValidator profileValidator,
                              IdentityVerificationService identityVerificationService,
                              EncryptionDecryptionUtil encryptionDecryptionUtil,
                              AddressRepository addressRepository,
                              UserDocumentRepository userDocumentRepository,
                              AddressMasterDataService addressMasterDataService,
                              AddressService addressService,
                              UserDocumentService userDocumentService,
                              UserConfiguration userConfiguration,
                              FamilyRestRepository familyRestRepository) {
        this.userRepository = userRepository;
        this.profileValidator = profileValidator;
        this.identityVerificationService = identityVerificationService;
        this.encryptionDecryptionUtil = encryptionDecryptionUtil;
        this.addressRepository = addressRepository;
        this.userDocumentRepository = userDocumentRepository;
        this.addressMasterDataService = addressMasterDataService;
        this.addressService = addressService;
        this.userDocumentService = userDocumentService;
        this.userConfiguration = userConfiguration;
        this.familyRestRepository = familyRestRepository;
    }

    /**
     * Test constructor with addressMasterDataService.
     *
     * @param userRepository              repository for user persistence
     * @param profileValidator            validator for profile request lifecycle rules
     * @param identityVerificationService adapter service for UIDAI Aadhaar verification
     * @param encryptionDecryptionUtil    utility for PII encryption/decryption
     * @param addressRepository           repository for address persistence
     * @param userDocumentRepository      repository for document proof persistence
     * @param addressMasterDataService    service for MDMS address boundary resolution
     */
    public ProfileServiceImpl(UserRepository userRepository,
                              ProfileValidator profileValidator,
                              IdentityVerificationService identityVerificationService,
                              EncryptionDecryptionUtil encryptionDecryptionUtil,
                              AddressRepository addressRepository,
                              UserDocumentRepository userDocumentRepository,
                              AddressMasterDataService addressMasterDataService) {
        this(userRepository, profileValidator, identityVerificationService, encryptionDecryptionUtil, addressRepository, userDocumentRepository, addressMasterDataService, null, null);
    }

    /**
     * Minimal test constructor.
     *
     * @param userRepository              repository for user persistence
     * @param profileValidator            validator for profile request lifecycle rules
     * @param identityVerificationService adapter service for UIDAI Aadhaar verification
     * @param encryptionDecryptionUtil    utility for PII encryption/decryption
     * @param addressRepository           repository for address persistence
     * @param userDocumentRepository      repository for document proof persistence
     */
    public ProfileServiceImpl(UserRepository userRepository,
                              ProfileValidator profileValidator,
                              IdentityVerificationService identityVerificationService,
                              EncryptionDecryptionUtil encryptionDecryptionUtil,
                              AddressRepository addressRepository,
                              UserDocumentRepository userDocumentRepository) {
        this(userRepository, profileValidator, identityVerificationService, encryptionDecryptionUtil, addressRepository, userDocumentRepository, null, null, null);
    }

    /**
     * Creates a complete citizen profile with all Section 8 mandatory fields.
     * Persists citizen user, address details, and document proofs.
     *
     * @param request profile create request payload
     * @return created citizen user profile with decrypted details
     * @throws CustomException if mandatory fields are missing or user already exists
     */
    @Override
    public User createProfile(ProfileCreateRequest request) {
        if (request != null) {
            request.unpackUserIfPresent();
        }
        log.info("[PROFILE-SERVICE] Creating citizen profile for mobile: {}, tenant: {}",
                MaskUtils.maskMobile(request != null ? request.getMobileNumber() : null),
                request != null ? request.getTenantId() : null);

        profileValidator.validateCreate(request);

        String stateTenant = resolveStateTenant(request.getTenantId());
        String mobile = request.getMobileNumber().trim();

        UserType userType = UserType.CITIZEN;
        if (StringUtils.isNotBlank(request.getType())) {
            try {
                userType = UserType.valueOf(request.getType().trim().toUpperCase());
            } catch (Exception ignored) {
                userType = UserType.CITIZEN;
            }
        }

        Boolean incomingIsVerified = request.getIsIdentityVerified() != null
                ? request.getIsIdentityVerified()
                : false;

        if (userType == UserType.CITIZEN) {
            // 1. Check if user already exists or if Aadhaar is linked to another citizen
            validateAadhaarUniqueness(request.getAadhaarNumber(), null, stateTenant);

            List<User> existingCitizens = findCitizensByMobile(mobile, stateTenant, request.getRequestInfo());
            long existingUnverifiedCount = 0;
            for (User u : existingCitizens) {
                if (StringUtils.isNotBlank(request.getAadhaarNumber()) && StringUtils.isNotBlank(u.getAadhaarNumber())
                        && request.getAadhaarNumber().trim().equals(u.getAadhaarNumber().trim())) {
                    log.error("[PROFILE-SERVICE] Citizen already exists with Aadhaar number in tenant: {}", stateTenant);
                    throw new CustomException(UserConstants.ERROR_USER_ALREADY_EXISTS, UserConstants.MSG_USER_ALREADY_EXISTS_AADHAAR);
                }
                if (StringUtils.equalsIgnoreCase(StringUtils.trim(request.getName()), StringUtils.trim(u.getName()))
                        && request.getDob() != null && u.getDob() != null
                        && isSameDate(request.getDob(), u.getDob())) {
                    log.error("[PROFILE-SERVICE] Citizen already exists with same name and DOB for mobile: {}", MaskUtils.maskMobile(mobile));
                    throw new CustomException(UserConstants.ERROR_USER_ALREADY_EXISTS, UserConstants.MSG_USER_ALREADY_EXISTS_DEMO);
                }
                if (u != null && (u.getActive() == null || Boolean.TRUE.equals(u.getActive()))
                        && !Boolean.TRUE.equals(u.getIsIdentityVerified())) {
                    existingUnverifiedCount++;
                }
            }

            // Enforce constraint: Only one citizen account with isVerified == false is permitted per mobile number.
            // All other citizen accounts under this mobile number must have isVerified == true.
            if (!Boolean.TRUE.equals(incomingIsVerified)) {
                if (existingUnverifiedCount > 0) {
                    log.error("[PROFILE-SERVICE] Cannot create unverified citizen: An unverified citizen account already exists for mobile: {}", MaskUtils.maskMobile(mobile));
                    throw new CustomException(UserConstants.ERROR_UNVERIFIED_CITIZEN_EXISTS,
                            UserConstants.MSG_UNVERIFIED_CITIZEN_EXISTS);
                }
            } else {
                if (existingUnverifiedCount > 1) {
                    log.error("[PROFILE-SERVICE] Cannot create citizen: Multiple unverified citizens already exist for mobile: {}", MaskUtils.maskMobile(mobile));
                    throw new CustomException(UserConstants.ERROR_UNVERIFIED_CITIZEN_EXISTS,
                            UserConstants.MSG_UNVERIFIED_CITIZEN_EXISTS);
                }
            }
        }

        // 2. Build Citizen User entity with all Section 8 fields
        Long loggedInUserId = resolveLoggedInUserId(request.getRequestInfo(), 0L);
        String username = !userRepository.isUserPresent(mobile, stateTenant, userType)
                ? mobile
                : mobile + "_" + UUID.randomUUID().toString().substring(0, 8);

        User newCitizen = User.builder()
                .username(username)
                .mobileNumber(mobile)
                .name(request.getName().trim())
                .fatherName(request.getFatherName() != null ? request.getFatherName().trim() : null)
                .motherName(request.getMotherName() != null ? request.getMotherName().trim() : null)
                .spouseName(request.getSpouseName() != null ? request.getSpouseName().trim() : null)
                .guardian(request.getGuardian() != null ? request.getGuardian().trim() : null)
                .guardianRelation(parseGuardianRelation(request.getGuardianRelation()))
                .alternateMobileNumber(request.getAlternateMobileNumber() != null ? request.getAlternateMobileNumber().trim() : null)
                .dob(request.getDob())
                .emailId(request.getEmailId() != null ? request.getEmailId().trim() : null)
                .gender(parseGender(request.getGender()))
                .aadhaarNumber(request.getAadhaarNumber() != null ? request.getAadhaarNumber().trim() : null)
                .photo(request.getPhoto())
                .tenantId(stateTenant)
                .type(userType)
                .active(true)
                .isIdentityVerified(incomingIsVerified)
                .identityVerifiedDate(Boolean.TRUE.equals(incomingIsVerified) ? new Date() : null)
                .identityVerifiedSource(Boolean.TRUE.equals(incomingIsVerified) ? UserConstants.IDENTITY_SOURCE_SYSTEM : null)
                .edittedCount(0)
                .roles(userType == UserType.CITIZEN ? Collections.singleton(Role.getCitizenRole()) : Collections.emptySet())
                .password(UUID.randomUUID().toString())
                .uuid(UUID.randomUUID().toString())
                .createdBy(loggedInUserId)
                .lastModifiedBy(loggedInUserId)
                .createdDate(new Date())
                .lastModifiedDate(new Date())
                .build();

        // 3. Encrypt and persist user
        User encryptedUser = encryptUser(newCitizen);
        User savedUser = userRepository.create(encryptedUser);
        log.info("[PROFILE-SERVICE] Successfully persisted citizen user ID: {}, UUID: {}",
                savedUser.getId(), savedUser.getUuid());

        // 4. Persist Address(es) (Enforce Single Active Permanent Address via SCD Type 2)
        List<Address> addressesToPersist = request.getEffectiveAddresses();
        Set<Address> savedAddresses = new LinkedHashSet<>();
        for (Address addr : addressesToPersist) {
            if (addr == null) continue;
            addr.setTenantId(request.getTenantId());
            addr.setUserId(savedUser.getId());
            addr.setUserUuid(savedUser.getUuid());
            addr.setLastModifiedBy(savedUser.getId());
            String typeToValidate = StringUtils.isNotBlank(addr.getAddressType())
                    ? addr.getAddressType()
                    : (addr.getType() != null ? addr.getType().name() : null);
            if (addressMasterDataService != null && StringUtils.isNotBlank(typeToValidate)) {
                addressMasterDataService.validateAddressType(request.getRequestInfo(), request.getTenantId(), typeToValidate);
            }
            if (addr.getType() == null && StringUtils.isNotBlank(addr.getAddressType())) {
                addr.setType(AddressType.fromValue(addr.getAddressType()));
            } else if (addr.getType() == null) {
                addr.setType(AddressType.CORRESPONDENCE);
            }
            if (StringUtils.isBlank(addr.getFilestoreId()) && StringUtils.isNotBlank(request.getAddressProofFileStoreId())) {
                addr.setFilestoreId(request.getAddressProofFileStoreId());
            }

            // Invalidate existing active permanent address if new address is PERMANENT
            if (AddressType.PERMANENT.equals(addr.getType())) {
                AddressSearchCriteria permCriteria = AddressSearchCriteria.builder()
                        .userId(savedUser.getId())
                        .addressType(AddressType.PERMANENT)
                        .status(UserConstants.ADDRESS_ACTIVE_STATUS)
                        .build();
                List<Address> activePerms = addressRepository.getAddressV2(permCriteria);
                if (!CollectionUtils.isEmpty(activePerms)) {
                    for (Address p : activePerms) {
                        log.info("[PROFILE-SERVICE] Inactivating previous permanent address ID: {} for user: {}", p.getId(), savedUser.getUuid());
                        addressRepository.updateAddressV2(p.getId(), savedUser.getId(), UserConstants.ADDRESS_INACTIVE_STATUS);
                    }
                }
            }

            Address savedAddress = addressRepository.createAddressV2(addr, savedUser.getId(), request.getTenantId());
            Address finalAddr = savedAddress != null ? savedAddress : addr;
            savedAddresses.add(finalAddr);

            // Also persist address proof into user document repository if present
            String userDocTenant = savedUser.getTenantId();
            if (StringUtils.isNotBlank(finalAddr.getFilestoreId())) {
                UserDocument addrDoc = UserDocument.builder()
                        .userId(savedUser.getId())
                        .userUuid(savedUser.getUuid())
                        .tenantId(userDocTenant)
                        .documentType(UserConstants.DOC_TYPE_ADDRESS_PROOF)
                        .filestoreId(finalAddr.getFilestoreId())
                        .active(true)
                        .createdBy(savedUser.getId())
                        .lastModifiedBy(savedUser.getId())
                        .build();
                userDocumentRepository.create(addrDoc);
            }
        }
        savedUser.setAddresses(savedAddresses);

        // 5. Persist Document Proofs
        String userDocTenant = savedUser.getTenantId();
        Set<String> attachedDocTypes = new HashSet<>();
        if (!CollectionUtils.isEmpty(request.getDocuments())) {
            for (UserDocument doc : request.getDocuments()) {
                if (doc != null) {
                    doc.setUserId(savedUser.getId());
                    doc.setUserUuid(savedUser.getUuid());
                    doc.setTenantId(userDocTenant);
                    doc.setCreatedBy(savedUser.getId());
                    doc.setLastModifiedBy(savedUser.getId());
                    if (doc.getActive() == null) {
                        doc.setActive(true);
                    }
                    userDocumentRepository.create(doc);
                    if (StringUtils.isNotBlank(doc.getDocumentType())) {
                        attachedDocTypes.add(doc.getDocumentType().trim().toUpperCase());
                    }
                }
            }
        }

        if (StringUtils.isNotBlank(request.getAddressProofFileStoreId()) && !containsDocType(attachedDocTypes, "ADDRESS_PROOF", "ADDRESS")) {
            UserDocument doc = UserDocument.builder()
                    .userId(savedUser.getId())
                    .userUuid(savedUser.getUuid())
                    .tenantId(userDocTenant)
                    .documentType("ADDRESS_PROOF")
                    .filestoreId(request.getAddressProofFileStoreId())
                    .active(true)
                    .createdBy(savedUser.getId())
                    .lastModifiedBy(savedUser.getId())
                    .build();
            userDocumentRepository.create(doc);
        }

        if (StringUtils.isNotBlank(request.getIdentityProofFileStoreId()) && !containsDocType(attachedDocTypes, "IDENTITY_PROOF", "IDENTITY")) {
            UserDocument doc = UserDocument.builder()
                    .userId(savedUser.getId())
                    .userUuid(savedUser.getUuid())
                    .tenantId(userDocTenant)
                    .documentType("IDENTITY_PROOF")
                    .filestoreId(request.getIdentityProofFileStoreId())
                    .active(true)
                    .createdBy(savedUser.getId())
                    .lastModifiedBy(savedUser.getId())
                    .build();
            userDocumentRepository.create(doc);
        }

        if (StringUtils.isNotBlank(request.getDobProofFileStoreId()) && !containsDocType(attachedDocTypes, "DOB_PROOF", "DOB")) {
            UserDocument doc = UserDocument.builder()
                    .userId(savedUser.getId())
                    .userUuid(savedUser.getUuid())
                    .tenantId(userDocTenant)
                    .documentType("DOB_PROOF")
                    .filestoreId(request.getDobProofFileStoreId())
                    .active(true)
                    .createdBy(savedUser.getId())
                    .lastModifiedBy(savedUser.getId())
                    .build();
            userDocumentRepository.create(doc);
        }

        // 6. Return decrypted response
        User decryptedUser = decryptUser(savedUser, request.getRequestInfo());
        if (savedUser.getAddresses() != null) {
            decryptedUser.setAddresses(savedUser.getAddresses());
        }
        enrichFamilyId(decryptedUser, stateTenant, request.getRequestInfo());
        return decryptedUser;
    }

    /**
     * Helper to verify if any of the target document types are present in the attached documents set.
     *
     * @param attachedTypes set of document types attached to the profile
     * @param matchTypes    variable arguments of document types to match against
     * @return true if at least one matching document type is found in the set, false otherwise
     */
    private boolean containsDocType(Set<String> attachedTypes, String... matchTypes) {
        if (CollectionUtils.isEmpty(attachedTypes)) return false;
        for (String m : matchTypes) {
            if (attachedTypes.contains(m.toUpperCase())) return true;
        }
        return false;
    }

    /**
     * Initializes a lightweight citizen profile with minimum fields (mobile, tenant, optional name/email).
     * Returns existing citizen profile if already registered under the state tenant.
     *
     * @param request profile init request payload
     * @return initialized or existing citizen user profile with decrypted details
     * @throws CustomException if validation fails or tenant/mobile are missing
     */
    @Override
    public User initProfile(ProfileInitRequest request) {
        if (request != null) {
            request.unpackUserIfPresent();
        }
        log.info("[PROFILE-SERVICE] Initializing profile for mobile: {}, tenant: {}",
                MaskUtils.maskMobile(request != null ? request.getMobileNumber() : null),
                request != null ? request.getTenantId() : null);

        profileValidator.validateInit(request);

        String stateTenant = resolveStateTenant(request.getTenantId());
        String mobile = request.getMobileNumber().trim();

        // 1. Check if user already exists
        User existingUser = findCitizenByMobile(mobile, stateTenant);
        if (existingUser != null) {
            log.info("[PROFILE-SERVICE] Matched existing citizen ID: {}, UUID: {} for mobile: {}",
                    existingUser.getId(), existingUser.getUuid(), MaskUtils.maskMobile(mobile));
            User decrypted = decryptUser(existingUser, request.getRequestInfo());
            enrichFamilyId(decrypted, stateTenant, request.getRequestInfo());
            return decrypted;
        }

        // 2. Provision lightweight citizen profile
        String defaultName = StringUtils.isNotBlank(request.getName()) ? request.getName().trim() : "Citizen";
        String username = !userRepository.isUserPresent(mobile, stateTenant, UserType.CITIZEN)
                ? mobile
                : mobile + "_" + UUID.randomUUID().toString().substring(0, 8);
        User newCitizen = User.builder()
                .username(username)
                .mobileNumber(mobile)
                .name(defaultName)
                .emailId(request.getEmailId())
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .active(true)
                .isIdentityVerified(false)
                .edittedCount(0)
                .roles(Collections.singleton(Role.getCitizenRole()))
                .password(UUID.randomUUID().toString())
                .uuid(UUID.randomUUID().toString())
                .build();

        // 3. Encrypt and persist
        User encryptedUser = encryptUser(newCitizen);
        User savedUser = userRepository.create(encryptedUser);
        log.info("[PROFILE-SERVICE] Successfully initialized citizen profile ID: {}, UUID: {}",
                savedUser.getId(), savedUser.getUuid());

        return decryptUser(savedUser, request.getRequestInfo());
    }

    /**
     * Updates citizen demographic profile adhering to Section 8 non-editable field rules.
     * Non-editable fields (mobile, father name, mother name, email) are strictly rejected.
     * Core identity fields (name, DOB, gender) can only be refreshed when authenticated via Aadhaar re-sync.
     *
     * @param request profile update request payload
     * @return refreshed citizen domain {@link User} object with decrypted fields
     * @throws CustomException if validation fails, data mismatch occurs, or unauthorized modification attempted
     */
    @Override
    public User updateProfile(ProfileUpdateRequest request) {
        log.info("[PROFILE-SERVICE] Processing profile update. isResync: {}",
                request != null ? request.getIsResync() : false);

        if (request == null || request.getEffectiveProfile() == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_UPDATE_REQUEST, UserConstants.MSG_UPDATE_BODY_NULL);
        }

        UserRequest incoming = request.getEffectiveProfile();
        User rawExistingUser = findTargetUser(incoming.getUuid(), incoming.getId(), incoming.getMobileNumber(), incoming.getTenantId());
        User existingUser = decryptUser(rawExistingUser, request.getRequestInfo());

        profileValidator.validateUpdate(request, existingUser);

        boolean isResync = Boolean.TRUE.equals(request.getIsResync());
        User userToUpdate = buildUpdatedUser(existingUser, incoming);
        userToUpdate.setId(rawExistingUser.getId());
        userToUpdate.setUuid(rawExistingUser.getUuid());
        userToUpdate.setUsername(rawExistingUser.getUsername());
        userToUpdate.setTenantId(rawExistingUser.getTenantId());

        String aadhaarToCheck = isResync
                ? (StringUtils.isNotBlank(request.getAadhaarNumber()) ? request.getAadhaarNumber() : existingUser.getAadhaarNumber())
                : incoming.getAadhaarNumber();
        if (StringUtils.isNotBlank(aadhaarToCheck)) {
            validateAadhaarUniqueness(aadhaarToCheck, existingUser.getUuid(), existingUser.getTenantId());
        }

        if (isResync) {
            log.info("[PROFILE-SERVICE] Executing Aadhaar re-sync for citizen UUID: {}", rawExistingUser.getUuid());
            String aadhaar = StringUtils.isNotBlank(request.getAadhaarNumber())
                    ? request.getAadhaarNumber()
                    : existingUser.getAadhaarNumber();
            String refId = StringUtils.isNotBlank(request.getTxnId()) ? request.getTxnId() : UUID.randomUUID().toString();

            VerifiedAadhaarDemographics eKyc = identityVerificationService.verifyAadhaarOtp(
                    request.getRequestInfo(),
                    existingUser.getTenantId(),
                    refId,
                    aadhaar,
                    request.getOtp(),
                    true
            );

            // Re-sync verified demographics
            if (StringUtils.isNotBlank(eKyc.getName())) {
                userToUpdate.setName(eKyc.getName());
            }
            if (eKyc.getDob() != null) {
                userToUpdate.setDob(eKyc.getDob());
            }
            if (eKyc.getGender() != null) {
                userToUpdate.setGender(eKyc.getGender());
            }
            if (StringUtils.isNotBlank(eKyc.getMaskedAadhaar())) {
                userToUpdate.setAadhaarNumber(eKyc.getMaskedAadhaar());
            }
            userToUpdate.setIsIdentityVerified(existingUser.getIsIdentityVerified());
            userToUpdate.setIdentityVerifiedDate(existingUser.getIdentityVerifiedDate());
            userToUpdate.setIdentityVerifiedSource(existingUser.getIdentityVerifiedSource());
            userToUpdate.setLastResyncDate(new Date());
        } else {
            // Standard update: identity verification fields MUST NOT be modified
            userToUpdate.setIsIdentityVerified(existingUser.getIsIdentityVerified());
            userToUpdate.setIdentityVerifiedDate(existingUser.getIdentityVerifiedDate());
            userToUpdate.setIdentityVerifiedSource(existingUser.getIdentityVerifiedSource());

            // allow updating name, dob, gender if unverified OR if demographic field was not set
            if (!Boolean.TRUE.equals(existingUser.getIsIdentityVerified())) {
                if (StringUtils.isNotBlank(incoming.getName())) {
                    userToUpdate.setName(incoming.getName());
                }
                if (incoming.getDob() != null) {
                    userToUpdate.setDob(incoming.getDob());
                }
                if (StringUtils.isNotBlank(incoming.getGender())) {
                    userToUpdate.setGender(parseGender(incoming.getGender()));
                }
            } else {
                // If verified citizen had null DOB or Gender in DB initially, permit enrichment
                if (existingUser.getDob() == null && incoming.getDob() != null) {
                    userToUpdate.setDob(incoming.getDob());
                }
                if (existingUser.getGender() == null && StringUtils.isNotBlank(incoming.getGender())) {
                    userToUpdate.setGender(parseGender(incoming.getGender()));
                }
            }
        }

        // Increment editted count
        int currentEdits = existingUser.getEdittedCount() != null ? existingUser.getEdittedCount() : 0;
        userToUpdate.setEdittedCount(currentEdits + 1);

        // Resolve updater info
        Long updaterId = resolveLoggedInUserId(request.getRequestInfo(), rawExistingUser.getId());
        String updaterUuid = resolveLoggedInUserUuid(request.getRequestInfo(), rawExistingUser.getUuid());

        User encryptedToUpdate = encryptUser(userToUpdate);
        userRepository.update(encryptedToUpdate, rawExistingUser, updaterId, updaterUuid);
        log.info("[PROFILE-SERVICE] Citizen profile successfully updated. ID: {}, UUID: {}, new editCount: {}",
                rawExistingUser.getId(), rawExistingUser.getUuid(), userToUpdate.getEdittedCount());

        User refreshedUser = findTargetUser(rawExistingUser.getUuid(), rawExistingUser.getId(), null, rawExistingUser.getTenantId());
        User decryptedUser = decryptUser(refreshedUser, request.getRequestInfo());
        enrichFamilyId(decryptedUser, rawExistingUser.getTenantId(), request.getRequestInfo());
        return decryptedUser;
    }

    /**
     * Resolves and normalizes requested entity inclusion flags (PROFILE, ADDRESS, DOCUMENT).
     * Filters nulls and defaults to [PROFILE] if the input list is null or empty.
     *
     * @param rawIncludes list of {@link ProfileIncludeEntity} sections requested
     * @return set of normalized {@link ProfileIncludeEntity} constants
     */
    @Override
    public Set<ProfileIncludeEntity> resolveIncludes(List<ProfileIncludeEntity> rawIncludes) {
        if (CollectionUtils.isEmpty(rawIncludes)) {
            return Collections.singleton(ProfileIncludeEntity.PROFILE);
        }
        Set<ProfileIncludeEntity> resolved = rawIncludes.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return resolved.isEmpty() ? Collections.singleton(ProfileIncludeEntity.PROFILE) : resolved;
    }

    /**
     * Searches citizen profiles based on supplied criteria filters (UUID, mobile, Aadhaar, name).
     * Automatically encrypts search criteria to query secure database indexes, decrypts the resulting
     * user records, and enriches related entities based on requested inclusion flags.
     *
     * @param request profile search request payload
     * @return list of decrypted citizen domain {@link User} profiles matching the criteria
     * @throws CustomException if search criteria are invalid or empty
     */
    @Override
    public List<User> searchProfile(ProfileSearchRequest request) {
        Set<ProfileIncludeEntity> includes = resolveIncludes(request != null ? request.getInclude() : null);
        return searchProfile(request, includes, null);
    }

    /**
     * Searches citizen profiles based on supplied criteria filters using pre-resolved inclusion sections,
     * avoiding duplicate resolution between controller and service layers.
     *
     * @param request  profile search request payload
     * @param includes pre-resolved entity sections to enrich
     * @return list of decrypted citizen domain {@link User} profiles matching the criteria
     * @throws CustomException if search criteria are invalid, empty, or if an enrichment error occurs
     */
    @Override
    public List<User> searchProfile(ProfileSearchRequest request, Set<ProfileIncludeEntity> includes) {
        return searchProfile(request, includes, null);
    }

    /**
     * Searches citizen profiles based on supplied criteria filters using pre-resolved inclusion sections,
     * collecting any partial enrichment errors (such as address or document service failures) into
     * {@code errors} so that a partial response with demographic profiles can be returned.
     *
     * @param request  profile search request payload
     * @param includes pre-resolved entity sections to enrich
     * @param errors   optional list to accumulate enrichment errors; if null, errors throw {@link CustomException}
     * @return list of decrypted citizen domain {@link User} profiles matching the criteria
     * @throws CustomException if search criteria are invalid, empty, or if an enrichment error occurs when errors is null
     */
    @Override
    public List<User> searchProfile(ProfileSearchRequest request, Set<ProfileIncludeEntity> includes, List<Error> errors) {
        log.info("[PROFILE-SERVICE] Searching citizen profiles with filters: tenant={}, mobile={}, aadhaar={}, include={}",
                request != null ? request.getTenantId() : null,
                MaskUtils.maskMobile(request != null ? request.getMobileNumber() : null),
                MaskUtils.maskAadhaar(request != null ? request.getAadhaarNumber() : null),
                includes);

        profileValidator.validateSearch(request);

        String stateTenant = resolveStateTenant(request.getTenantId());
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .tenantId(stateTenant)
                .uuid(StringUtils.isNotBlank(request.getUuid()) ? Collections.singletonList(request.getUuid().trim()) : null)
                .mobileNumber(request.getMobileNumber())
                .aadhaarNumber(request.getAadhaarNumber())
                .name(request.getName())
                .active(request.getActive() != null ? request.getActive() : true)
                .type(UserType.CITIZEN)
                .excludeAddressDetails(true)
                .build();

        criteria = encryptSearchCriteria(criteria);
        List<User> userList = userRepository.findAll(criteria);
        if (CollectionUtils.isEmpty(userList)) {
            return Collections.emptyList();
        }

        List<User> decryptedUsers = new ArrayList<>();
        for (User matchedUser : userList) {
            decryptedUsers.add(decryptUser(matchedUser, request.getRequestInfo()));
        }

        Boolean active = (request != null && request.getActive() != null) ? request.getActive() : Boolean.TRUE;
        for (User user : decryptedUsers) {
            enrichRelatedEntities(user, stateTenant, includes, request != null ? request.getRequestInfo() : null, active, errors);
        }

        return decryptedUsers;
    }

    /**
     * Enriches retrieved citizen user record with related entity collections (addresses, documents)
     * based on requested inclusion flags.
     * If {@code active} is specified (e.g. true), only matching active child records (addresses and documents)
     * are enriched and returned.
     * If an error occurs while enriching an included section and {@code errors} list is provided,
     * the error is captured into {@code errors} allowing partial degradation. Otherwise, a {@link CustomException} is thrown.
     *
     * @param user         decrypted user entity to enrich
     * @param stateTenant  resolved state-level tenant identifier
     * @param includes     set of entity sections to include (e.g. ADDRESS, DOCUMENT)
     * @param requestInfo  UPYOG request audit context
     * @param active       optional active status filter from search request
     * @param errors       optional list to collect partial enrichment errors
     * @throws CustomException if enriching address or document entities fails and errors list is null
     */
    private void enrichRelatedEntities(User user, String stateTenant, Set<ProfileIncludeEntity> includes, RequestInfo requestInfo, Boolean active, List<Error> errors) {
        if (user == null) {
            return;
        }

        // Dynamically enrich familyId for citizen from upyog-family-service (Single Source of Truth)
        enrichFamilyId(user, stateTenant, requestInfo);

        if (CollectionUtils.isEmpty(includes)) {
            return;
        }

        boolean includeAddresses = includes.contains(ProfileIncludeEntity.ADDRESS);
        boolean includeDocuments = includes.contains(ProfileIncludeEntity.DOCUMENT);

        if (!includeAddresses && !includeDocuments) {
            return;
        }

        if (includeAddresses) {
            try {
                AddressSearchCriteria addrCriteria = AddressSearchCriteria.builder()
                        .uuid(user.getUuid())
                        .status(Boolean.TRUE.equals(active) ? UserConstants.ADDRESS_ACTIVE_STATUS : (Boolean.FALSE.equals(active) ? UserConstants.ADDRESS_INACTIVE_STATUS : null))
                        .build();

                List<Address> addresses = null;
                if (addressService != null) {
                    addresses = addressService.searchAddress(addrCriteria, requestInfo);
                } else if (addressRepository != null) {
                    addresses = addressRepository.getAddressV2(addrCriteria);
                }

                if (!CollectionUtils.isEmpty(addresses)) {
                    if (Boolean.TRUE.equals(active)) {
                        addresses = addresses.stream()
                                .filter(a -> a.getStatus() == null || UserConstants.ADDRESS_ACTIVE_STATUS.equalsIgnoreCase(a.getStatus()))
                                .collect(Collectors.toList());
                    } else if (Boolean.FALSE.equals(active)) {
                        addresses = addresses.stream()
                                .filter(a -> UserConstants.ADDRESS_INACTIVE_STATUS.equalsIgnoreCase(a.getStatus()))
                                .collect(Collectors.toList());
                    }

                    for (Address addr : addresses) {
                        if (StringUtils.isBlank(addr.getUserUuid())) {
                            addr.setUserUuid(user.getUuid());
                        }
                    }

                    user.setAddresses(new LinkedHashSet<>(addresses));
                }
            } catch (Exception e) {
                log.error("[PROFILE-SERVICE] Error enriching addresses for profile search: {}", e.getMessage(), e);
                if (errors != null) {
                    Error err = new Error();
                    err.setCode("ADDRESS_ENRICHMENT_FAILED");
                    err.setMessage("Failed to enrich address details for citizen profile search: " + e.getMessage());
                    err.setDescription(e.getMessage());
                    errors.add(err);
                } else {
                    if (e instanceof CustomException) {
                        throw (CustomException) e;
                    }
                    throw new CustomException("ADDRESS_ENRICHMENT_FAILED", "Failed to enrich address details for citizen profile search: " + e.getMessage());
                }
            }
        }

        if (includeDocuments) {
            try {
                UserDocumentSearchCriteria docCriteria = UserDocumentSearchCriteria.builder()
                        .uuid(user.getUuid())
                        .tenantId(stateTenant)
                        .active(active)
                        .build();

                List<UserDocument> documents = null;
                if (userDocumentService != null) {
                    documents = userDocumentService.searchDocuments(docCriteria, requestInfo);
                } else if (userDocumentRepository != null) {
                    documents = userDocumentRepository.search(docCriteria);
                }

                if (!CollectionUtils.isEmpty(documents)) {
                    if (Boolean.TRUE.equals(active)) {
                        documents = documents.stream()
                                .filter(d -> d.getActive() == null || Boolean.TRUE.equals(d.getActive()))
                                .collect(Collectors.toList());
                    } else if (Boolean.FALSE.equals(active)) {
                        documents = documents.stream()
                                .filter(d -> Boolean.FALSE.equals(d.getActive()))
                                .collect(Collectors.toList());
                    }

                    for (UserDocument doc : documents) {
                        if (StringUtils.isBlank(doc.getUserUuid())) {
                            doc.setUserUuid(user.getUuid());
                        }
                    }

                    user.setDocuments(documents);
                }
            } catch (Exception e) {
                log.error("[PROFILE-SERVICE] Error enriching documents for profile search: {}", e.getMessage(), e);
                if (errors != null) {
                    Error err = new Error();
                    err.setCode("DOCUMENT_ENRICHMENT_FAILED");
                    err.setMessage("Failed to enrich document proofs for citizen profile search: " + e.getMessage());
                    err.setDescription(e.getMessage());
                    errors.add(err);
                } else {
                    if (e instanceof CustomException) {
                        throw (CustomException) e;
                    }
                    throw new CustomException("DOCUMENT_ENRICHMENT_FAILED", "Failed to enrich document proofs for citizen profile search: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Resolves and enriches active familyId from upyog-family-service for a single citizen user.
     */
    private void enrichFamilyId(User user, String tenantId, RequestInfo requestInfo) {
        if (familyRestRepository == null || user == null || StringUtils.isBlank(user.getUuid())) {
            return;
        }
        try {
            String familyId = familyRestRepository.getActiveFamilyIdByUserUuid(user.getUuid(), tenantId, requestInfo);
            if (StringUtils.isNotBlank(familyId)) {
                user.setFamilyId(familyId);
            }
        } catch (Exception e) {
            log.warn("[PROFILE-SERVICE] Non-blocking: Failed to enrich familyId from family service for user {}: {}",
                    user.getUuid(), e.getMessage());
        }
    }

    /**
     * Re-synchronizes citizen demographics with Aadhaar e-KYC using OTP authentication.
     * Overwrites verified name, DOB, gender, and masked Aadhaar directly from the identity verification provider.
     * Increments profile edit count and records the resync timestamp.
     *
     * @param request profile re-sync request payload containing OTP and transaction identifier
     * @return updated citizen domain {@link User} profile with refreshed demographics
     * @throws CustomException if OTP verification fails or target citizen record does not exist
     */
    @Override
    public User resyncProfile(ProfileResyncRequest request) {
        log.info("[PROFILE-SERVICE] Explicit profile re-sync requested for citizen: uuid={}, mobile={}",
                request != null ? request.getUuid() : null,
                MaskUtils.maskMobile(request != null ? request.getMobileNumber() : null));

        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_RESYNC_REQUEST, UserConstants.MSG_RESYNC_BODY_NULL);
        }
        request.unpackUserIfPresent();

        User rawExistingUser = findTargetUser(request.getUuid(), request.getId(), request.getMobileNumber(), request.getTenantId());
        User existingUser = decryptUser(rawExistingUser, request.getRequestInfo());
        profileValidator.validateResync(request, existingUser);

        String aadhaar = StringUtils.isNotBlank(request.getAadhaarNumber())
                ? request.getAadhaarNumber()
                : existingUser.getAadhaarNumber();
        validateAadhaarUniqueness(aadhaar, existingUser.getUuid(), existingUser.getTenantId());
        String refId = StringUtils.isNotBlank(request.getTxnId()) ? request.getTxnId() : UUID.randomUUID().toString();

        VerifiedAadhaarDemographics eKyc = identityVerificationService.verifyAadhaarOtp(
                request.getRequestInfo(),
                existingUser.getTenantId(),
                refId,
                aadhaar,
                request.getOtp(),
                true
        );

        User userToUpdate = copyUser(existingUser);
        userToUpdate.setId(rawExistingUser.getId());
        userToUpdate.setUuid(rawExistingUser.getUuid());
        userToUpdate.setUsername(rawExistingUser.getUsername());
        userToUpdate.setTenantId(rawExistingUser.getTenantId());
        if (StringUtils.isNotBlank(eKyc.getName())) {
            userToUpdate.setName(eKyc.getName());
        }
        if (eKyc.getDob() != null) {
            userToUpdate.setDob(eKyc.getDob());
        }
        if (eKyc.getGender() != null) {
            userToUpdate.setGender(eKyc.getGender());
        }
        if (StringUtils.isNotBlank(eKyc.getMaskedAadhaar())) {
            userToUpdate.setAadhaarNumber(eKyc.getMaskedAadhaar());
        }

        userToUpdate.setIsIdentityVerified(true);
        userToUpdate.setIdentityVerifiedDate(existingUser.getIdentityVerifiedDate() != null
                ? existingUser.getIdentityVerifiedDate() : new Date());
        userToUpdate.setIdentityVerifiedSource(UserConstants.IDENTITY_SOURCE_AADHAAR);
        userToUpdate.setLastResyncDate(new Date());

        int currentEdits = existingUser.getEdittedCount() != null ? existingUser.getEdittedCount() : 0;
        userToUpdate.setEdittedCount(currentEdits + 1);

        Long updaterId = resolveLoggedInUserId(request.getRequestInfo(), rawExistingUser.getId());
        String updaterUuid = resolveLoggedInUserUuid(request.getRequestInfo(), rawExistingUser.getUuid());

        User encryptedToUpdate = encryptUser(userToUpdate);
        userRepository.update(encryptedToUpdate, rawExistingUser, updaterId, updaterUuid);
        log.info("[PROFILE-SERVICE] Aadhaar re-sync successful for citizen UUID: {}, new lastResyncDate: {}",
                rawExistingUser.getUuid(), userToUpdate.getLastResyncDate());

        User refreshedUser = findTargetUser(rawExistingUser.getUuid(), rawExistingUser.getId(), null, rawExistingUser.getTenantId());
        User decryptedUser = decryptUser(refreshedUser, request.getRequestInfo());
        enrichFamilyId(decryptedUser, rawExistingUser.getTenantId(), request.getRequestInfo());
        return decryptedUser;
    }

    /**
     * Builds an updated domain {@link User} by cloning existing attributes and overlaying editable fields
     * from the incoming {@link UserRequest}.
     *
     * @param existingUser the current decrypted user from database
     * @param incoming     the update request containing requested modifications
     * @return a modified domain {@link User} object ready for persistence
     */
    private User buildUpdatedUser(User existingUser, UserRequest incoming) {
        User updated = copyUser(existingUser);

        if (StringUtils.isNotBlank(incoming.getSalutation())) {
            updated.setSalutation(incoming.getSalutation());
        }
        if (incoming.getFatherName() != null) {
            updated.setFatherName(incoming.getFatherName());
        }
        if (incoming.getMotherName() != null) {
            updated.setMotherName(incoming.getMotherName());
        }
        if (incoming.getSpouseName() != null) {
            updated.setSpouseName(incoming.getSpouseName());
        }
        if (incoming.getEmailId() != null) {
            updated.setEmailId(incoming.getEmailId());
        }
        if (incoming.getAltContactNumber() != null) {
            updated.setAltContactNumber(incoming.getAltContactNumber());
        }
        if (StringUtils.isNotBlank(incoming.getAlternatemobilenumber())) {
            updated.setAlternateMobileNumber(incoming.getAlternatemobilenumber());
        }
        if (incoming.getPan() != null) {
            updated.setPan(incoming.getPan());
        }
        if (incoming.getFatherOrHusbandName() != null) {
            updated.setGuardian(incoming.getFatherOrHusbandName());
        }
        if (incoming.getRelationship() != null) {
            updated.setGuardianRelation(incoming.getRelationship());
        }
        if (incoming.getBloodGroup() != null) {
            updated.setBloodGroup(parseBloodGroup(incoming.getBloodGroup()));
        }
        if (incoming.getPhoto() != null) {
            updated.setPhoto(incoming.getPhoto());
        }
        if (incoming.getSignature() != null) {
            updated.setSignature(incoming.getSignature());
        }
        if (incoming.getIdentificationMark() != null) {
            updated.setIdentificationMark(incoming.getIdentificationMark());
        }
        if (StringUtils.isNotBlank(incoming.getLocale())) {
            updated.setLocale(incoming.getLocale());
        }
        if (StringUtils.isNotBlank(incoming.getDigilockerid())) {
            updated.setDigilockerid(incoming.getDigilockerid());
        }

        // Addresses are decoupled from eg_user and managed via dedicated Address APIs
        updated.setPermanentAddress(null);
        updated.setCorrespondenceAddress(null);
        updated.setAddresses(null);

        return updated;
    }

    /**
     * Creates a deep-copy clone of a domain {@link User} instance.
     *
     * @param source the source user to clone
     * @return a new {@link User} instance with identical fields
     */
    private User copyUser(User source) {
        return User.builder()
                .id(source.getId())
                .uuid(source.getUuid())
                .username(source.getUsername())
                .title(source.getTitle())
                .salutation(source.getSalutation())
                .name(source.getName())
                .gender(source.getGender())
                .mobileNumber(source.getMobileNumber())
                .alternateMobileNumber(source.getAlternateMobileNumber())
                .emailId(source.getEmailId())
                .altContactNumber(source.getAltContactNumber())
                .pan(source.getPan())
                .aadhaarNumber(source.getAadhaarNumber())
                .guardian(source.getGuardian())
                .guardianRelation(source.getGuardianRelation())
                .active(source.getActive())
                .dob(source.getDob())
                .locale(source.getLocale())
                .type(source.getType())
                .tenantId(source.getTenantId())
                .roles(source.getRoles())
                .fatherName(source.getFatherName())
                .motherName(source.getMotherName())
                .spouseName(source.getSpouseName())
                .isIdentityVerified(source.getIsIdentityVerified())
                .identityVerifiedDate(source.getIdentityVerifiedDate())
                .identityVerifiedSource(source.getIdentityVerifiedSource())
                .lastResyncDate(source.getLastResyncDate())
                .edittedCount(source.getEdittedCount())
                .lastLoginMethod(source.getLastLoginMethod())
                .photo(source.getPhoto())
                .signature(source.getSignature())
                .identificationMark(source.getIdentificationMark())
                .bloodGroup(source.getBloodGroup())
                .digilockerid(source.getDigilockerid())
                .password(source.getPassword())
                .passwordExpiryDate(source.getPasswordExpiryDate())
                .accountLocked(source.getAccountLocked())
                .accountLockedDate(source.getAccountLockedDate())
                .build();
    }

    /**
     * Resolves the target citizen {@link User} record from the database by UUID, numeric ID, or mobile number,
     * verifying cross-attribute integrity between supplied and persisted identifiers.
     *
     * @param uuid     optional citizen UUID
     * @param id       optional citizen numeric ID
     * @param mobile   optional citizen mobile number
     * @param tenantId tenant identifier
     * @return matching domain {@link User} record
     * @throws CustomException if no user is found or if provided ID and UUID conflict with database state
     */
    private User findTargetUser(String uuid, Long id, String mobile, String tenantId) {
        String stateTenant = resolveStateTenant(tenantId);
        UserSearchCriteria.UserSearchCriteriaBuilder builder = UserSearchCriteria.builder()
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .excludeAddressDetails(true);

        if (StringUtils.isNotBlank(uuid)) {
            builder.uuid(Collections.singletonList(uuid));
        } else if (id != null) {
            builder.id(Collections.singletonList(id));
        } else if (StringUtils.isNotBlank(mobile)) {
            builder.mobileNumber(mobile);
        } else {
            throw new CustomException(UserConstants.ERROR_USER_IDENTIFIER_REQUIRED, UserConstants.MSG_USER_IDENTIFIER_REQUIRED);
        }

        List<User> list = userRepository.findAll(builder.build());
        if (CollectionUtils.isEmpty(list)) {
            throw new CustomException(UserConstants.ERROR_DATA_MISMATCH, "Data mismatch: Citizen profile not found for identifier (id: " + id + ", uuid: " + uuid + ")");
        }
        User existing = list.get(0);
        if (id != null && existing.getId() != null && !id.equals(existing.getId())) {
            log.error("[PROFILE-SERVICE] Data mismatch: provided id {} does not match database id {}", id, existing.getId());
            throw new CustomException(UserConstants.ERROR_DATA_MISMATCH, "Data mismatch: Provided id " + id + " does not match database record for uuid: " + uuid);
        }
        if (StringUtils.isNotBlank(uuid) && StringUtils.isNotBlank(existing.getUuid()) && !uuid.equalsIgnoreCase(existing.getUuid())) {
            log.error("[PROFILE-SERVICE] Data mismatch: provided uuid {} does not match database uuid {}", uuid, existing.getUuid());
            throw new CustomException(UserConstants.ERROR_DATA_MISMATCH, "Data mismatch: Provided uuid " + uuid + " does not match database record for id: " + id);
        }
        return existing;
    }

    /**
     * Searches for a single citizen user associated with a mobile number in the given state tenant.
     *
     * @param mobile      citizen mobile number
     * @param stateTenant state tenant identifier
     * @return the first matched decrypted citizen {@link User}, or {@code null} if none exist
     */
    private User findCitizenByMobile(String mobile, String stateTenant) {
        List<User> citizens = findCitizensByMobile(mobile, stateTenant, null);
        return !CollectionUtils.isEmpty(citizens) ? citizens.get(0) : null;
    }

    /**
     * Resolves all citizen profiles associated with a mobile number across encrypted mobile and username columns.
     *
     * @param mobile      citizen 10-digit mobile number
     * @param stateTenant state tenant identifier
     * @param requestInfo request context for decryption
     * @return list of decrypted citizen domain {@link User} profiles
     */
    private List<User> findCitizensByMobile(String mobile, String stateTenant, RequestInfo requestInfo) {
        Map<String, User> userMap = new LinkedHashMap<>();

        // 1. Search by encrypted mobileNumber
        UserSearchCriteria mobileCriteria = UserSearchCriteria.builder()
                .mobileNumber(mobile)
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .build();
        try {
            UserSearchCriteria encCriteria = encryptSearchCriteria(mobileCriteria);
            List<User> usersByMobile = userRepository.findAll(encCriteria != null ? encCriteria : mobileCriteria);
            if (!CollectionUtils.isEmpty(usersByMobile)) {
                for (User u : usersByMobile) {
                    if (u.getUuid() != null) {
                        userMap.put(u.getUuid(), u);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[PROFILE-SERVICE] Error querying citizens by encrypted mobile: {}", e.getMessage());
        }

        // 2. Search by encrypted userName
        UserSearchCriteria userCriteria = UserSearchCriteria.builder()
                .userName(mobile)
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .build();
        try {
            UserSearchCriteria encCriteria = encryptSearchCriteria(userCriteria);
            List<User> usersByUserName = userRepository.findAll(encCriteria != null ? encCriteria : userCriteria);
            if (!CollectionUtils.isEmpty(usersByUserName)) {
                for (User u : usersByUserName) {
                    if (u.getUuid() != null) {
                        userMap.put(u.getUuid(), u);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[PROFILE-SERVICE] Error querying citizens by encrypted userName: {}", e.getMessage());
        }

        if (userMap.isEmpty()) {
            return Collections.emptyList();
        }

        return decryptUserList(new ArrayList<>(userMap.values()), requestInfo);
    }

    /**
     * Checks if two dates fall on the exact same calendar year, month, and day of month.
     *
     * @param d1 first date
     * @param d2 second date
     * @return true if both dates represent the same calendar day, false otherwise
     */
    private boolean isSameDate(Date d1, Date d2) {
        if (d1 == null && d2 == null) return true;
        if (d1 == null || d2 == null) return false;
        Calendar c1 = Calendar.getInstance();
        c1.setTime(d1);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(d2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
                && c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Encrypts PII fields of a domain {@link User} object using the User encryption configuration.
     * Prevents duplicate re-encryption of fields that already contain ciphertext ("keyId|ciphertext").
     *
     * @param user the domain user containing plaintext or partially encrypted fields
     * @return encrypted domain user instance
     */
    private User encryptUser(User user) {
        if (user == null) return null;

        // CRITICAL: Preserve any fields that are already in encrypted format ("keyId|ciphertext")
        // to prevent duplicate encryption which bloats ciphertext beyond column limits.
        String existingEncAadhaar = isEncryptedFormat(user.getAadhaarNumber()) ? user.getAadhaarNumber() : null;
        String existingEncName = isEncryptedFormat(user.getName()) ? user.getName() : null;
        String existingEncMobile = isEncryptedFormat(user.getMobileNumber()) ? user.getMobileNumber() : null;
        String existingEncEmail = isEncryptedFormat(user.getEmailId()) ? user.getEmailId() : null;
        String existingEncPan = isEncryptedFormat(user.getPan()) ? user.getPan() : null;
        String existingEncAltContact = isEncryptedFormat(user.getAltContactNumber()) ? user.getAltContactNumber() : null;
        String existingEncAltMobile = isEncryptedFormat(user.getAlternateMobileNumber()) ? user.getAlternateMobileNumber() : null;
        String existingEncGuardian = isEncryptedFormat(user.getGuardian()) ? user.getGuardian() : null;
        String existingEncUsername = isEncryptedFormat(user.getUsername()) ? user.getUsername() : null;

        if (existingEncAadhaar != null) user.setAadhaarNumber(null);
        if (existingEncName != null) user.setName(null);
        if (existingEncMobile != null) user.setMobileNumber(null);
        if (existingEncEmail != null) user.setEmailId(null);
        if (existingEncPan != null) user.setPan(null);
        if (existingEncAltContact != null) user.setAltContactNumber(null);
        if (existingEncAltMobile != null) user.setAlternateMobileNumber(null);
        if (existingEncGuardian != null) user.setGuardian(null);
        if (existingEncUsername != null) user.setUsername(null);

        User encrypted;
        try {
            encrypted = encryptionDecryptionUtil.encryptObject(user, UserConstants.USER_ENCRYPTION_KEY, User.class);
        } catch (Exception e) {
            log.warn("[PROFILE-SERVICE] Encryption failed, proceeding unencrypted in dev mode: {}", e.getMessage());
            encrypted = user;
        }

        if (existingEncAadhaar != null) {
            encrypted.setAadhaarNumber(existingEncAadhaar);
            user.setAadhaarNumber(existingEncAadhaar);
        }
        if (existingEncName != null) {
            encrypted.setName(existingEncName);
            user.setName(existingEncName);
        }
        if (existingEncMobile != null) {
            encrypted.setMobileNumber(existingEncMobile);
            user.setMobileNumber(existingEncMobile);
        }
        if (existingEncEmail != null) {
            encrypted.setEmailId(existingEncEmail);
            user.setEmailId(existingEncEmail);
        }
        if (existingEncPan != null) {
            encrypted.setPan(existingEncPan);
            user.setPan(existingEncPan);
        }
        if (existingEncAltContact != null) {
            encrypted.setAltContactNumber(existingEncAltContact);
            user.setAltContactNumber(existingEncAltContact);
        }
        if (existingEncAltMobile != null) {
            encrypted.setAlternateMobileNumber(existingEncAltMobile);
            user.setAlternateMobileNumber(existingEncAltMobile);
        }
        if (existingEncGuardian != null) {
            encrypted.setGuardian(existingEncGuardian);
            user.setGuardian(existingEncGuardian);
        }
        if (existingEncUsername != null) {
            encrypted.setUsername(existingEncUsername);
            user.setUsername(existingEncUsername);
        }

        return encrypted;
    }

    /**
     * Determines whether a given string is already in standard UPYOG encrypted format ("keyId|ciphertext").
     *
     * @param value raw candidate string
     * @return true if format matches keyId|ciphertext, false otherwise
     */
    private boolean isEncryptedFormat(String value) {
        if (StringUtils.isBlank(value)) return false;
        String[] parts = value.split("\\|");
        if (parts.length != 2) return false;
        try {
            Integer.parseInt(parts[0].trim());
            return !parts[1].trim().isEmpty();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Encrypts fields in the search criteria to facilitate querying against encrypted database columns.
     *
     * @param criteria user search criteria with plain attributes
     * @return criteria with encrypted filter fields
     */
    private UserSearchCriteria encryptSearchCriteria(UserSearchCriteria criteria) {
        try {
            return encryptionDecryptionUtil.encryptObject(criteria, UserConstants.USER_ENCRYPTION_KEY, UserSearchCriteria.class);
        } catch (Exception e) {
            log.warn("[PROFILE-SERVICE] Search criteria encryption skipped: {}", e.getMessage());
            return criteria;
        }
    }

    /**
     * Decrypts a single domain {@link User} entity using the UPYOG decryption service.
     *
     * @param user        encrypted user record
     * @param requestInfo request context with user credentials
     * @return decrypted {@link User} entity or raw input on failure
     */
    private User decryptUser(User user, RequestInfo requestInfo) {
        if (user == null) return null;
        try {
            List<User> decrypted = encryptionDecryptionUtil.decryptObject(Collections.singletonList(user), null, User.class, requestInfo);
            return !CollectionUtils.isEmpty(decrypted) ? decrypted.get(0) : user;
        } catch (Exception e) {
            log.warn("[PROFILE-SERVICE] Decryption fallback returned raw user: {}", e.getMessage());
            return user;
        }
    }

    /**
     * Decrypts a list of domain {@link User} entities.
     *
     * @param list        list of encrypted user records
     * @param requestInfo request context with user credentials
     * @return list of decrypted {@link User} entities
     */
    private List<User> decryptUserList(List<User> list, RequestInfo requestInfo) {
        if (CollectionUtils.isEmpty(list)) return Collections.emptyList();
        try {
            return encryptionDecryptionUtil.decryptObject(list, null, User.class, requestInfo);
        } catch (Exception e) {
            log.warn("[PROFILE-SERVICE] Decryption fallback returned raw list: {}", e.getMessage());
            return list;
        }
    }

    /**
     * Normalizes tenant identifier to state-level tenant code (e.g. "pb.amritsar" -> "pb").
     * If tenantId is blank, retrieves state-level tenant ID from userConfiguration.
     * Throws CustomException if getStateLevelTenantId() is null or blank.
     *
     * @param tenantId city or state tenant ID
     * @return state-level tenant identifier
     */
    private String resolveStateTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            if (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getStateLevelTenantId())) {
                return userConfiguration.getStateLevelTenantId().trim();
            }
            log.error("[PROFILE-SERVICE] Tenant ID cannot be determined: state-level tenant ID is not configured");
            throw new CustomException(UserConstants.ERROR_TENANT_ID_REQUIRED, "Tenant ID cannot be determined: state-level tenant ID is not configured");
        }
        return tenantId.contains(".") ? tenantId.split("\\.")[0] : tenantId;
    }

    /**
     * Extracts numeric user ID of currently logged-in actor from RequestInfo, falling back to default.
     *
     * @param requestInfo request context
     * @param fallback    fallback user ID
     * @return authenticated user ID or fallback
     */
    private Long resolveLoggedInUserId(RequestInfo requestInfo, Long fallback) {
        return (requestInfo != null && requestInfo.getUserInfo() != null && requestInfo.getUserInfo().getId() != null)
                ? requestInfo.getUserInfo().getId()
                : fallback;
    }

    /**
     * Extracts UUID string of currently logged-in actor from RequestInfo, falling back to default.
     *
     * @param requestInfo request context
     * @param fallback    fallback UUID string
     * @return authenticated user UUID or fallback
     */
    private String resolveLoggedInUserUuid(RequestInfo requestInfo, String fallback) {
        return (requestInfo != null && requestInfo.getUserInfo() != null && StringUtils.isNotBlank(requestInfo.getUserInfo().getUuid()))
                ? requestInfo.getUserInfo().getUuid()
                : fallback;
    }

    /**
     * Parses gender string into domain {@link Gender} enum, defaulting to {@link Gender#OTHERS} on mismatch.
     *
     * @param g gender string representation
     * @return resolved {@link Gender} enum or null if empty
     */
    private Gender parseGender(String g) {
        if (StringUtils.isBlank(g)) return null;
        try {
            return Gender.valueOf(g.trim().toUpperCase());
        } catch (Exception e) {
            return Gender.OTHERS;
        }
    }

    /**
     * Parses blood group string into {@link BloodGroup} enum.
     *
     * @param bg blood group string representation
     * @return resolved {@link BloodGroup} enum or null if empty/invalid
     */
    private BloodGroup parseBloodGroup(String bg) {
        if (StringUtils.isBlank(bg)) return null;
        try {
            BloodGroup b = BloodGroup.fromValue(bg.trim());
            if (b != null) return b;
            String normalized = bg.trim().toUpperCase().replace(" ", "_");
            return BloodGroup.valueOf(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses guardian relationship string into {@link GuardianRelation} enum.
     *
     * @param gr guardian relation string
     * @return resolved {@link GuardianRelation} enum or null if blank
     */
    private GuardianRelation parseGuardianRelation(String gr) {
        if (StringUtils.isBlank(gr)) return null;
        try {
            return GuardianRelation.valueOf(gr.trim().toUpperCase());
        } catch (Exception e) {
            return GuardianRelation.OTHER;
        }
    }

    /**
     * Validates that an Aadhaar number is unique across all citizen profiles in the tenant.
     * Prevents multiple citizens from sharing or claiming the same Aadhaar identity.
     *
     * @param rawAadhaar      the raw or masked 12-digit Aadhaar number
     * @param currentUserUuid the UUID of the citizen currently updating/resyncing, or null for new creation
     * @param stateTenant     the state-level tenant identifier
     * @throws CustomException if another citizen profile already possesses this Aadhaar number
     */
    private void validateAadhaarUniqueness(String rawAadhaar, String currentUserUuid, String stateTenant) {
        if (StringUtils.isBlank(rawAadhaar)) {
            return;
        }
        String maskedAadhaar = MaskUtils.maskAadhaar(rawAadhaar);
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .aadhaarNumber(maskedAadhaar)
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .active(true)
                .build();
        List<User> matchingUsers = userRepository.findAll(criteria);
        if (!CollectionUtils.isEmpty(matchingUsers)) {
            for (User u : matchingUsers) {
                if (currentUserUuid == null || (StringUtils.isNotBlank(u.getUuid()) && !u.getUuid().equalsIgnoreCase(currentUserUuid))) {
                    log.error("[PROFILE-SERVICE] Aadhaar uniqueness violation: Aadhaar {} is already linked to citizen UUID: {}",
                            maskedAadhaar, u.getUuid());
                    throw new CustomException(UserConstants.ERROR_AADHAAR_ALREADY_LINKED, UserConstants.MSG_AADHAAR_ALREADY_LINKED);
                }
            }
        }
    }

    /**
     * Verifies Aadhaar OTP via identity provider adapter and resolves citizen identity.
     * <p>
     * - If matching citizen exists: returns full profile details (name, mobileNumber, uuid, dob, gender,
     *   aadhaarNumber, active familyId) with isNewUser=false.
     * - If matching citizen does NOT exist: auto-provisions a new verified citizen account in eg_user
     *   from Aadhaar e-KYC demographics and returns full profile details with isNewUser=true.
     *
     * @param request Aadhaar citizen verification request containing referenceId, OTP, and optional Aadhaar/mobile
     * @return AadhaarCitizenVerifyResponse containing resolved citizen demographics, familyId, and isNewUser flag
     */
    @Override
    public AadhaarCitizenVerifyResponse verifyAndResolveAadhaarCitizen(AadhaarCitizenVerifyRequest request) {
        if (request == null) {
            log.error("[PROFILE-SERVICE] Aadhaar verification failed: request is null");
            throw new CustomException(UserConstants.ERROR_INVALID_REQUEST, "Aadhaar verification request cannot be null");
        }

        if (Boolean.TRUE.equals(request.getIsSelfVerification())) {
            log.info("[PROFILE-SERVICE] Request flagged as isSelfVerification=true. Delegating to verifyLoggedInCitizenProfile.");
            return verifyLoggedInCitizenProfile(request);
        }

        String refId = request.getEffectiveReferenceId();
        String rawAadhaar = request.getAadhaarNumber();
        String otp = request.getOtp();
        String tenantId = request.getTenantId();

        if (StringUtils.isBlank(otp)) {
            log.error("[PROFILE-SERVICE] Validation failed: OTP is missing");
            throw new CustomException(UserConstants.ERROR_INVALID_OTP, "OTP is required for Aadhaar verification");
        }

        if (StringUtils.isBlank(refId) && StringUtils.isBlank(rawAadhaar)) {
            log.error("[PROFILE-SERVICE] Validation failed: Neither referenceId nor aadhaarNumber provided");
            throw new CustomException(UserConstants.ERROR_INVALID_AADHAAR_IDENTIFIER, "Either referenceId or aadhaarNumber must be provided");
        }

        String stateTenant = resolveStateTenant(tenantId);
        log.info("[PROFILE-SERVICE] Verifying Aadhaar OTP for refId: {}, rawAadhaar: {}, stateTenant: {}",
                MaskUtils.maskIdentifier(refId), MaskUtils.maskAadhaar(rawAadhaar), stateTenant);

        // 1. Verify Aadhaar OTP with identity provider
        VerifiedAadhaarDemographics eKyc = identityVerificationService.verifyAadhaarOtp(
                request.getRequestInfo(),
                stateTenant,
                refId,
                rawAadhaar,
                otp,
                false
        );

        if (eKyc == null || !eKyc.isVerified()) {
            log.error("[PROFILE-SERVICE] Aadhaar verification unsuccessful from identity provider");
            throw new CustomException(UserConstants.ERROR_AADHAAR_VERIFICATION_FAILED, "Aadhaar OTP verification failed");
        }

        String searchAadhaar = StringUtils.isNotBlank(eKyc.getMaskedAadhaar())
                ? eKyc.getMaskedAadhaar()
                : MaskUtils.maskAadhaar(rawAadhaar);

        // Security: only e-KYC verified mobile is trusted for existing account lookup to prevent account takeover
        String verifiedEkycMobile = StringUtils.isNotBlank(eKyc.getMobileNumber()) ? eKyc.getMobileNumber().trim() : null;

        log.info("[PROFILE-SERVICE] e-KYC verified: maskedAadhaar={}", searchAadhaar);

        // 2. Search for existing citizen using only verified e-KYC credentials (Aadhaar or e-KYC mobile)
        User existingUser = findExistingCitizen(searchAadhaar, rawAadhaar, verifiedEkycMobile, stateTenant, request.getRequestInfo());

        if (existingUser != null) {
            log.info("[PROFILE-SERVICE] Found existing citizen UUID: {} for verified Aadhaar", existingUser.getUuid());

            if (!Boolean.TRUE.equals(existingUser.getActive())) {
                log.error("[PROFILE-SERVICE] Citizen account {} is inactive", existingUser.getUuid());
                throw new CustomException(UserConstants.ERROR_ACCOUNT_INACTIVE, "Citizen account is inactive");
            }

            // Update verification status & demographics if necessary
            existingUser.setIsIdentityVerified(true);
            existingUser.setIdentityVerifiedDate(new Date());
            existingUser.setIdentityVerifiedSource(UserConstants.IDENTITY_SOURCE_AADHAAR);
            existingUser.setLastResyncDate(new Date());
            existingUser.setLastLoginMethod(AuthType.AADHAAR_NUMBER.name());

            if (StringUtils.isNotBlank(searchAadhaar)) {
                existingUser.setAadhaarNumber(searchAadhaar);
            }
            if (StringUtils.isNotBlank(eKyc.getName()) && ("Citizen".equalsIgnoreCase(existingUser.getName()) || StringUtils.isBlank(existingUser.getName()))) {
                existingUser.setName(eKyc.getName());
            }
            if (existingUser.getDob() == null && eKyc.getDob() != null) {
                existingUser.setDob(eKyc.getDob());
            }
            if (existingUser.getGender() == null && eKyc.getGender() != null) {
                existingUser.setGender(eKyc.getGender());
            }

            try {
                User encryptedToUpdate = encryptUser(existingUser);
                userRepository.update(encryptedToUpdate, existingUser, existingUser.getId(), existingUser.getUuid());
                log.info("[PROFILE-SERVICE] Updated identity verification attributes for citizen UUID: {}", existingUser.getUuid());
            } catch (Exception e) {
                log.error("[PROFILE-SERVICE] Failed to persist updated verification fields for user {}: {}",
                        existingUser.getUuid(), e.getMessage());
                throw new CustomException(UserConstants.ERROR_INVALID_REQUEST,
                        "Failed to persist identity verification details: " + e.getMessage());
            }

            User decryptedUser = decryptUser(existingUser, request.getRequestInfo());
            enrichFamilyId(decryptedUser, stateTenant, request.getRequestInfo());

            UserSearchResponseContent content = new UserSearchResponseContent(decryptedUser);
            return AadhaarCitizenVerifyResponse.builder()
                    .responseInfo(createResponseInfo(request.getRequestInfo(), UserConstants.STATUS_SUCCESS))
                    .user(content)
                    .isNewUser(false)
                    .build();
        }

        // 3. User does not exist -> Auto-provision new verified citizen profile
        log.info("[PROFILE-SERVICE] Citizen not found for verified Aadhaar. Auto-provisioning new citizen profile.");

        String mobileNumber = verifiedEkycMobile;
        if (mobileNumber == null && StringUtils.isNotBlank(request.getMobileNumber())) {
            mobileNumber = request.getMobileNumber().trim();
        }

        boolean hasMobile = StringUtils.isNotBlank(mobileNumber);
        String baseUsername;
        if (hasMobile) {
            baseUsername = mobileNumber;
        } else if (searchAadhaar != null) {
            baseUsername = UserConstants.AADHAAR_USERNAME_PREFIX + searchAadhaar.replaceAll("\\D", "");
        } else {
            baseUsername = UserConstants.AADHAAR_USERNAME_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        }
        String username = !userRepository.isUserPresent(baseUsername, stateTenant, UserType.CITIZEN)
                ? baseUsername
                : baseUsername + "_" + UUID.randomUUID().toString().substring(0, 8);

        String citizenName = resolveCitizenDisplayName(eKyc.getName(), request.getName());

        Long loggedInUserId = resolveLoggedInUserId(request.getRequestInfo(), 0L);

        User newCitizen = User.builder()
                .username(username)
                .name(citizenName)
                .mobileNumber(hasMobile ? mobileNumber : null)
                .gender(eKyc.getGender())
                .dob(eKyc.getDob())
                .aadhaarNumber(searchAadhaar)
                .photo(eKyc.getPhoto())
                .isIdentityVerified(true)
                .identityVerifiedDate(new Date())
                .identityVerifiedSource(UserConstants.IDENTITY_SOURCE_AADHAAR)
                .lastResyncDate(new Date())
                .lastLoginMethod(AuthType.AADHAAR_NUMBER.name())
                .edittedCount(0)
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Collections.singleton(Role.getCitizenRole()))
                .mobileValidationMandatory(hasMobile)
                .otpValidationMandatory(false)
                .password(UUID.randomUUID().toString())
                .uuid(UUID.randomUUID().toString())
                .createdBy(loggedInUserId)
                .lastModifiedBy(loggedInUserId)
                .createdDate(new Date())
                .lastModifiedDate(new Date())
                .build();

        User encryptedUser = encryptUser(newCitizen);
        User savedUser = userRepository.create(encryptedUser);
        log.info("[PROFILE-SERVICE] Successfully auto-provisioned Aadhaar citizen ID: {}, UUID: {}",
                savedUser.getId(), savedUser.getUuid());

        User decryptedUser = decryptUser(savedUser, request.getRequestInfo());
        enrichFamilyId(decryptedUser, stateTenant, request.getRequestInfo());

        UserSearchResponseContent content = new UserSearchResponseContent(decryptedUser);
        return AadhaarCitizenVerifyResponse.builder()
                .responseInfo(createResponseInfo(request.getRequestInfo(), UserConstants.STATUS_SUCCESS))
                .user(content)
                .isNewUser(true)
                .build();
    }

    /**
     * Verifies Aadhaar OTP for an authenticated citizen who logged in with their mobile number,
     * ensuring that the mobile number registered with Aadhaar matches the logged-in user's mobile number.
     * On match, marks the citizen's profile as identity-verified with Aadhaar, updates e-KYC demographics,
     * links the masked Aadhaar number, persists changes to the database, and returns the verified profile response.
     *
     * @param request Aadhaar citizen verification request containing referenceId, OTP, and RequestInfo
     * @return AadhaarCitizenVerifyResponse containing verified citizen profile details and identity verification status
     * @throws CustomException if mobile numbers do not match or Aadhaar is already linked to another account
     */
    @Override
    public AadhaarCitizenVerifyResponse verifyLoggedInCitizenProfile(AadhaarCitizenVerifyRequest request) {
        validateVerifyLoggedInProfileRequest(request);

        String callerUuid = request.getRequestInfo().getUserInfo().getUuid();
        String refId = request.getEffectiveReferenceId();
        String rawAadhaar = request.getAadhaarNumber();
        String tenantId = StringUtils.isNotBlank(request.getTenantId())
                ? request.getTenantId()
                : request.getRequestInfo().getUserInfo().getTenantId();

        String stateTenant = resolveStateTenant(tenantId);
        log.info("[PROFILE-SERVICE] Verifying Aadhaar for logged-in citizen: uuid={}, refId={}, stateTenant={}",
                callerUuid, MaskUtils.maskIdentifier(refId), stateTenant);

        // 1. Fetch logged-in user profile from database
        User rawExistingUser = findTargetUser(callerUuid, null, null, stateTenant);
        if (!Boolean.TRUE.equals(rawExistingUser.getActive())) {
            log.error("[PROFILE-SERVICE] Logged-in citizen account {} is inactive", rawExistingUser.getUuid());
            throw new CustomException(UserConstants.ERROR_ACCOUNT_INACTIVE, "Citizen account is inactive");
        }

        User existingUser = decryptUser(rawExistingUser, request.getRequestInfo());

        // 2. Verify Aadhaar OTP via identity provider
        VerifiedAadhaarDemographics eKyc = identityVerificationService.verifyAadhaarOtp(
                request.getRequestInfo(),
                stateTenant,
                refId,
                rawAadhaar,
                request.getOtp(),
                false
        );

        if (eKyc == null || !eKyc.isVerified()) {
            log.error("[PROFILE-SERVICE] Aadhaar verification unsuccessful from identity provider for user: {}", callerUuid);
            throw new CustomException(UserConstants.ERROR_AADHAAR_VERIFICATION_FAILED, "Aadhaar OTP verification failed");
        }

        String searchAadhaar = StringUtils.isNotBlank(eKyc.getMaskedAadhaar())
                ? eKyc.getMaskedAadhaar()
                : MaskUtils.maskAadhaar(rawAadhaar);

        // 3. Aadhaar uniqueness validation across citizen accounts
        validateAadhaarUniqueness(searchAadhaar, callerUuid, stateTenant);

        // 4. Strict mobile number matching check
        String loggedInMobile = resolveLoggedInMobile(existingUser, rawExistingUser, request.getRequestInfo());
       
        String aadhaarMobile = eKyc.getMobileNumber();

        if (userConfiguration != null && (Boolean.TRUE.equals(userConfiguration.getAadhaarMockEnabled()) || Boolean.TRUE.equals(userConfiguration.getAadhaarGatewayMockEnabled()))) {
            log.info("[PROFILE-SERVICE] Mock Aadhaar verification enabled. Considering user mobile equal to eKyc.getMobileNumber() ({}) by default for UUID: {}",
                    MaskUtils.maskMobile(loggedInMobile), callerUuid);
            aadhaarMobile = loggedInMobile;
            if (StringUtils.isNotBlank(loggedInMobile) && StringUtils.isBlank(eKyc.getMobileNumber())) {
                eKyc.setMobileNumber(loggedInMobile);
            }
        }

        log.info("[PROFILE-SERVICE] Validating Aadhaar mobile match for UUID {}: loggedInMobile={}, aadhaarMobileFound={}",
                callerUuid, MaskUtils.maskMobile(loggedInMobile), StringUtils.isNotBlank(aadhaarMobile));

        if (!isMobileMatching(loggedInMobile, aadhaarMobile)) {
            log.error("[PROFILE-SERVICE] Aadhaar mobile mismatch for UUID {}: logged-in mobile ({}) does not match Aadhaar mobile ({})",
                    callerUuid, MaskUtils.maskMobile(loggedInMobile), MaskUtils.maskMobile(aadhaarMobile));
            throw new CustomException(UserConstants.ERROR_AADHAAR_MOBILE_MISMATCH, UserConstants.MSG_AADHAAR_MOBILE_MISMATCH);
        }

        // 5. Update citizen profile with Aadhaar verification details and demographics
        updateAadhaarVerifiedFields(existingUser, searchAadhaar, eKyc);

        try {
            User encryptedToUpdate = encryptUser(existingUser);
            Long updaterId = resolveLoggedInUserId(request.getRequestInfo(), rawExistingUser.getId() != null ? rawExistingUser.getId() : 0L);
            userRepository.update(encryptedToUpdate, rawExistingUser, updaterId, callerUuid);
            log.info("[PROFILE-SERVICE] Successfully updated verified Aadhaar details in eg_user table for citizen UUID: {}", rawExistingUser.getUuid());
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[PROFILE-SERVICE] Failed to persist verified Aadhaar details for citizen {}: {}",
                    rawExistingUser.getUuid(), e.getMessage());
            throw new CustomException(UserConstants.ERROR_INVALID_REQUEST,
                    "Failed to persist identity verification details: " + e.getMessage());
        }

        User decryptedUser = decryptUser(existingUser, request.getRequestInfo());
        enrichFamilyId(decryptedUser, stateTenant, request.getRequestInfo());

        UserSearchResponseContent content = new UserSearchResponseContent(decryptedUser);
        return AadhaarCitizenVerifyResponse.builder()
                .responseInfo(createResponseInfo(request.getRequestInfo(), UserConstants.STATUS_SUCCESS))
                .user(content)
                .isNewUser(false)
                .build();
    }

    private void validateVerifyLoggedInProfileRequest(AadhaarCitizenVerifyRequest request) {
        if (request == null) {
            log.error("[PROFILE-SERVICE] Profile Aadhaar verification failed: request is null");
            throw new CustomException(UserConstants.ERROR_INVALID_REQUEST, "Aadhaar verification request cannot be null");
        }
        if (request.getRequestInfo() == null || request.getRequestInfo().getUserInfo() == null
                || StringUtils.isBlank(request.getRequestInfo().getUserInfo().getUuid())) {
            log.error("[PROFILE-SERVICE] Unauthorized: UserInfo or UUID missing in RequestInfo");
            throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS, "User must be authenticated to verify Aadhaar profile");
        }
        if (StringUtils.isBlank(request.getOtp())) {
            log.error("[PROFILE-SERVICE] Validation failed: OTP is missing");
            throw new CustomException(UserConstants.ERROR_INVALID_OTP, "OTP is required for Aadhaar verification");
        }
        if (StringUtils.isBlank(request.getEffectiveReferenceId()) && StringUtils.isBlank(request.getAadhaarNumber())) {
            log.error("[PROFILE-SERVICE] Validation failed: Neither referenceId nor aadhaarNumber provided");
            throw new CustomException(UserConstants.ERROR_INVALID_AADHAAR_IDENTIFIER, "Either referenceId or aadhaarNumber must be provided");
        }
    }

    private String resolveLoggedInMobile(User existingUser, User rawExistingUser, RequestInfo requestInfo) {
        if (StringUtils.isNotBlank(existingUser.getMobileNumber())) {
            return existingUser.getMobileNumber();
        }
        if (StringUtils.isNotBlank(rawExistingUser.getUsername()) && rawExistingUser.getUsername().matches("\\d{10}")) {
            return rawExistingUser.getUsername();
        }
        if (requestInfo != null && requestInfo.getUserInfo() != null && StringUtils.isNotBlank(requestInfo.getUserInfo().getMobileNumber())) {
            return requestInfo.getUserInfo().getMobileNumber();
        }
        return null;
    }

    private void updateAadhaarVerifiedFields(User existingUser, String searchAadhaar, VerifiedAadhaarDemographics eKyc) {
        existingUser.setIsIdentityVerified(true);
        existingUser.setIdentityVerifiedDate(new Date());
        existingUser.setIdentityVerifiedSource(UserConstants.IDENTITY_SOURCE_AADHAAR);
        existingUser.setLastResyncDate(new Date());
        existingUser.setLastLoginMethod(AuthType.AADHAAR_NUMBER.name());

        if (StringUtils.isNotBlank(searchAadhaar)) {
            existingUser.setAadhaarNumber(searchAadhaar);
        }
        if (StringUtils.isNotBlank(eKyc.getName())) {
            existingUser.setName(eKyc.getName());
        }
        if (eKyc.getDob() != null) {
            existingUser.setDob(eKyc.getDob());
        }
        if (eKyc.getGender() != null) {
            existingUser.setGender(eKyc.getGender());
        }
        if (StringUtils.isNotBlank(eKyc.getPhoto())) {
            existingUser.setPhoto(eKyc.getPhoto());
        }
    }

    private String resolveCitizenDisplayName(String eKycName, String requestName) {
        if (StringUtils.isNotBlank(eKycName)) {
            return eKycName;
        }
        if (StringUtils.isNotBlank(requestName)) {
            return requestName;
        }
        return "Citizen";
    }

    /**
     * Compares two mobile numbers ignoring non-digit characters, country code prefixes (+91 or 91),
     * and leading zeros, comparing the standard 10-digit mobile number.
     *
     * @param m1 first mobile number
     * @param m2 second mobile number
     * @return true if the last 10 digits match; false otherwise
     */
    private boolean isMobileMatching(String m1, String m2) {
        if (StringUtils.isBlank(m1) || StringUtils.isBlank(m2)) {
            return false;
        }
        String clean1 = cleanMobileNumber(m1);
        String clean2 = cleanMobileNumber(m2);
        return clean1.length() == 10 && clean1.equals(clean2);
    }

    /**
     * Normalizes a mobile number string by extracting digit characters and trimming to standard 10 digits.
     *
     * @param m raw mobile number string
     * @return extracted 10-digit mobile string or original digits if shorter
     */
    private String cleanMobileNumber(String m) {
        if (m == null) return "";
        String digits = m.replaceAll("\\D", "");
        if (digits.length() > 10) {
            return digits.substring(digits.length() - 10);
        }
        return digits;
    }

    /**
     * Searches for an existing citizen profile matching the verified Aadhaar or mobile number.
     *
     * @param maskedAadhaar masked 12-digit Aadhaar number
     * @param rawAadhaar    raw Aadhaar number if provided
     * @param mobileNumber  mobile number if provided
     * @param stateTenant   state-level tenant ID
     * @param requestInfo   request context for decryption
     * @return matched {@link User} entity or null if not found
     */
    private User findExistingCitizen(String maskedAadhaar, String rawAadhaar, String mobileNumber, String stateTenant, RequestInfo requestInfo) {
        // 1. Match by maskedAadhaar
        if (StringUtils.isNotBlank(maskedAadhaar)) {
            User found = findCitizenByAadhaar(maskedAadhaar, stateTenant, requestInfo);
            if (found != null) {
                return found;
            }
        }

        // 2. Match by rawAadhaar if distinct
        if (StringUtils.isNotBlank(rawAadhaar) && !rawAadhaar.equalsIgnoreCase(maskedAadhaar)) {
            User found = findCitizenByAadhaar(rawAadhaar, stateTenant, requestInfo);
            if (found != null) {
                return found;
            }
        }

        // 3. Match by mobileNumber
        if (StringUtils.isNotBlank(mobileNumber)) {
            List<User> matchedUsers = findCitizensByMobile(mobileNumber, stateTenant, requestInfo);
            if (!CollectionUtils.isEmpty(matchedUsers)) {
                // If any matched account has matching Aadhaar, prioritize it
                for (User u : matchedUsers) {
                    if (StringUtils.isNotBlank(u.getAadhaarNumber()) &&
                            (StringUtils.equalsIgnoreCase(u.getAadhaarNumber(), maskedAadhaar)
                             || StringUtils.equalsIgnoreCase(MaskUtils.maskAadhaar(u.getAadhaarNumber()), maskedAadhaar)
                             || (StringUtils.isNotBlank(rawAadhaar) && StringUtils.equalsIgnoreCase(u.getAadhaarNumber(), rawAadhaar)))) {
                        return u;
                    }
                }
                // If exactly 1 account exists on this mobile, match it ONLY if it does not already belong
                // to a verified citizen or a citizen with a distinct Aadhaar. Otherwise, in a family multi-account
                // setup, allow a new citizen account to be auto-provisioned under the shared mobile number.
                if (matchedUsers.size() == 1) {
                    User candidate = matchedUsers.get(0);
                    boolean isCandidateVerified = Boolean.TRUE.equals(candidate.getIsIdentityVerified());
                    boolean hasDistinctAadhaar = StringUtils.isNotBlank(candidate.getAadhaarNumber())
                            && !StringUtils.equalsIgnoreCase(candidate.getAadhaarNumber(), maskedAadhaar)
                            && !StringUtils.equalsIgnoreCase(MaskUtils.maskAadhaar(candidate.getAadhaarNumber()), maskedAadhaar);

                    if (!isCandidateVerified && !hasDistinctAadhaar) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Queries citizen profiles by Aadhaar number across the state tenant.
     *
     * @param aadhaar     raw or masked Aadhaar number
     * @param stateTenant state-level tenant identifier
     * @param requestInfo request context for decryption
     * @return matching active citizen {@link User} entity or null
     */
    private User findCitizenByAadhaar(String aadhaar, String stateTenant, RequestInfo requestInfo) {
        if (StringUtils.isBlank(aadhaar)) {
            return null;
        }
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .aadhaarNumber(aadhaar)
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .build();
        try {
            UserSearchCriteria encCriteria = encryptSearchCriteria(criteria);
            List<User> users = userRepository.findAll(encCriteria != null ? encCriteria : criteria);
            if (!CollectionUtils.isEmpty(users)) {
                List<User> decrypted = decryptUserList(users, requestInfo);
                List<User> resultList = !CollectionUtils.isEmpty(decrypted) ? decrypted : users;
                for (User u : resultList) {
                    if (Boolean.TRUE.equals(u.getActive())) {
                        return u;
                    }
                }
                return resultList.get(0);
            }
        } catch (Exception e) {
            log.warn("[PROFILE-SERVICE] Error searching citizen by Aadhaar: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Builds standard ResponseInfo envelope.
     */
    private ResponseInfo createResponseInfo(RequestInfo requestInfo, String status) {
        Long ts = (requestInfo != null && requestInfo.getTs() != null)
                ? requestInfo.getTs()
                : System.currentTimeMillis();
        return ResponseInfo.builder()
                .apiId(requestInfo != null ? requestInfo.getApiId() : null)
                .ver(requestInfo != null ? requestInfo.getVer() : null)
                .ts(ts)
                .resMsgId(UUID.randomUUID().toString())
                .msgId(requestInfo != null ? requestInfo.getMsgId() : null)
                .status(status)
                .build();
    }
}
