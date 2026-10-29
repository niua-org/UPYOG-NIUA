package org.egov.user.domain.service.profile;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
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
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.UserRequest;
import org.egov.user.web.contract.document.UserDocumentSearchCriteria;
import org.egov.user.web.contract.profile.ProfileCreateRequest;
import org.egov.user.web.contract.profile.ProfileIncludeEntity;
import org.egov.user.web.contract.profile.ProfileInitRequest;
import org.egov.user.web.contract.profile.ProfileResyncRequest;
import org.egov.user.web.contract.profile.ProfileSearchRequest;
import org.egov.user.web.contract.profile.ProfileUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    public ProfileServiceImpl(UserRepository userRepository,
                              ProfileValidator profileValidator,
                              IdentityVerificationService identityVerificationService,
                              EncryptionDecryptionUtil encryptionDecryptionUtil,
                              AddressRepository addressRepository,
                              UserDocumentRepository userDocumentRepository,
                              @Autowired(required = false) AddressMasterDataService addressMasterDataService,
                              @Autowired(required = false) AddressService addressService,
                              @Autowired(required = false) UserDocumentService userDocumentService) {
        this.userRepository = userRepository;
        this.profileValidator = profileValidator;
        this.identityVerificationService = identityVerificationService;
        this.encryptionDecryptionUtil = encryptionDecryptionUtil;
        this.addressRepository = addressRepository;
        this.userDocumentRepository = userDocumentRepository;
        this.addressMasterDataService = addressMasterDataService;
        this.addressService = addressService;
        this.userDocumentService = userDocumentService;
    }

    public ProfileServiceImpl(UserRepository userRepository,
                              ProfileValidator profileValidator,
                              IdentityVerificationService identityVerificationService,
                              EncryptionDecryptionUtil encryptionDecryptionUtil,
                              AddressRepository addressRepository,
                              UserDocumentRepository userDocumentRepository,
                              AddressMasterDataService addressMasterDataService) {
        this(userRepository, profileValidator, identityVerificationService, encryptionDecryptionUtil, addressRepository, userDocumentRepository, addressMasterDataService, null, null);
    }

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
                maskMobile(request != null ? request.getMobileNumber() : null),
                request != null ? request.getTenantId() : null);

        profileValidator.validateCreate(request);

        String stateTenant = resolveStateTenant(request.getTenantId());
        String mobile = request.getMobileNumber().trim();

        // 1. Check if user already exists or if Aadhaar is linked to another citizen
        validateAadhaarUniqueness(request.getAadhaarNumber(), null, stateTenant);

        List<User> existingCitizens = findCitizensByMobile(mobile, stateTenant, request.getRequestInfo());
        for (User u : existingCitizens) {
            if (StringUtils.isNotBlank(request.getAadhaarNumber()) && StringUtils.isNotBlank(u.getAadhaarNumber())
                    && request.getAadhaarNumber().trim().equals(u.getAadhaarNumber().trim())) {
                log.error("[PROFILE-SERVICE] Citizen already exists with Aadhaar number in tenant: {}", stateTenant);
                throw new CustomException(UserConstants.ERROR_USER_ALREADY_EXISTS, UserConstants.MSG_USER_ALREADY_EXISTS_AADHAAR);
            }
            if (StringUtils.equalsIgnoreCase(StringUtils.trim(request.getName()), StringUtils.trim(u.getName()))
                    && request.getDob() != null && u.getDob() != null
                    && isSameDate(request.getDob(), u.getDob())) {
                log.error("[PROFILE-SERVICE] Citizen already exists with same name and DOB for mobile: {}", maskMobile(mobile));
                throw new CustomException(UserConstants.ERROR_USER_ALREADY_EXISTS, UserConstants.MSG_USER_ALREADY_EXISTS_DEMO);
            }
        }

        // 2. Build Citizen User entity with all Section 8 fields
        Long loggedInUserId = resolveLoggedInUserId(request.getRequestInfo(), 0L);
        String username = !userRepository.isUserPresent(mobile, stateTenant, UserType.CITIZEN)
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
                .type(UserType.CITIZEN)
                .active(true)
                .isIdentityVerified(false)
                .edittedCount(0)
                .roles(Collections.singleton(Role.getCitizenRole()))
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
                maskMobile(request != null ? request.getMobileNumber() : null),
                request != null ? request.getTenantId() : null);

        profileValidator.validateInit(request);

        String stateTenant = resolveStateTenant(request.getTenantId());
        String mobile = request.getMobileNumber().trim();

        // 1. Check if user already exists
        User existingUser = findCitizenByMobile(mobile, stateTenant);
        if (existingUser != null) {
            log.info("[PROFILE-SERVICE] Matched existing citizen ID: {}, UUID: {} for mobile: {}",
                    existingUser.getId(), existingUser.getUuid(), maskMobile(mobile));
            return decryptUser(existingUser, request.getRequestInfo());
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
            userToUpdate.setIsIdentityVerified(true);
            userToUpdate.setIdentityVerifiedDate(existingUser.getIdentityVerifiedDate() != null
                    ? existingUser.getIdentityVerifiedDate() : new Date());
            userToUpdate.setLastResyncDate(new Date());
        } else {
            // Standard update: allow updating name, dob, gender if unverified OR if demographic field was not set
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
        return decryptUser(refreshedUser, request.getRequestInfo());
    }

    /**
     * Searches citizen profiles based on supplied criteria filters (UUID, ID, mobile, Aadhaar, name).
     * Automatically encrypts search criteria to query secure database indexes, and decrypts the resulting
     * user records before returning.
     *
     * @param request profile search request payload
     * @return list of decrypted citizen domain {@link User} profiles matching the criteria
     * @throws CustomException if search criteria are invalid or empty
     */
    @Override
    public Set<String> resolveIncludes(List<String> rawIncludes) {
        if (CollectionUtils.isEmpty(rawIncludes)) {
            return Collections.singleton(ProfileIncludeEntity.PROFILE.name());
        }
        Set<String> resolved = rawIncludes.stream()
                .filter(StringUtils::isNotBlank)
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toSet());
        return resolved.isEmpty() ? Collections.singleton(ProfileIncludeEntity.PROFILE.name()) : resolved;
    }

    @Override
    public List<User> searchProfile(ProfileSearchRequest request) {
        log.info("[PROFILE-SERVICE] Searching citizen profiles with filters: tenant={}, mobile={}, aadhaar={}, include={}",
                request != null ? request.getTenantId() : null,
                maskMobile(request != null ? request.getMobileNumber() : null),
                maskAadhaar(request != null ? request.getAadhaarNumber() : null),
                request != null ? request.getInclude() : null);

        profileValidator.validateSearch(request);

        String stateTenant = resolveStateTenant(request.getTenantId());
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .tenantId(stateTenant)
                .uuid(request.getUuid())
                .id(request.getId())
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

        List<User> decryptedUsers = decryptUserList(userList, request.getRequestInfo());
        Set<String> includes = resolveIncludes(request != null ? request.getInclude() : null);

        enrichRelatedEntities(decryptedUsers, stateTenant, includes, request != null ? request.getRequestInfo() : null);

        return decryptedUsers;
    }

    private void enrichRelatedEntities(List<User> users, String stateTenant, Set<String> includes, RequestInfo requestInfo) {
        if (CollectionUtils.isEmpty(users) || CollectionUtils.isEmpty(includes)) {
            return;
        }

        boolean includeAddresses = includes.contains(ProfileIncludeEntity.ADDRESS.name());
        boolean includeDocuments = includes.contains(ProfileIncludeEntity.DOCUMENT.name());

        if (!includeAddresses && !includeDocuments) {
            return;
        }

        for (User user : users) {
            if (StringUtils.isBlank(user.getUuid())) {
                continue;
            }

            if (includeAddresses) {
                try {
                    AddressSearchCriteria addrCriteria = AddressSearchCriteria.builder()
                            .uuid(user.getUuid())
                            .status(UserConstants.ADDRESS_ACTIVE_STATUS)
                            .build();

                    List<Address> addresses = null;
                    if (addressService != null) {
                        addresses = addressService.searchAddress(addrCriteria, requestInfo);
                    } else if (addressRepository != null) {
                        addresses = addressRepository.getAddressV2(addrCriteria);
                    }

                    if (addresses != null) {
                        user.setAddresses(new HashSet<>(addresses));
                    }
                } catch (Exception e) {
                    log.error("[PROFILE-SERVICE] Error fetching addresses for user UUID: {}", user.getUuid(), e);
                }
            }

            if (includeDocuments) {
                try {
                    UserDocumentSearchCriteria docCriteria = UserDocumentSearchCriteria.builder()
                            .uuid(user.getUuid())
                            .build();

                    List<UserDocument> documents = null;
                    if (userDocumentService != null) {
                        documents = userDocumentService.searchDocuments(docCriteria, requestInfo);
                    } else if (userDocumentRepository != null) {
                        documents = userDocumentRepository.search(docCriteria);
                    }

                    if (documents != null) {
                        user.setDocuments(documents);
                    }
                } catch (Exception e) {
                    log.error("[PROFILE-SERVICE] Error fetching documents for user UUID: {}", user.getUuid(), e);
                }
            }
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
                maskMobile(request != null ? request.getMobileNumber() : null));

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
        return decryptUser(refreshedUser, request.getRequestInfo());
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
     *
     * @param tenantId city or state tenant ID
     * @return state-level tenant identifier
     */
    private String resolveStateTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) return "pb";
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
     * Masks citizen mobile number for security logging (e.g. "98******10").
     *
     * @param mobile 10-digit mobile number
     * @return masked mobile string
     */
    private String maskMobile(String mobile) {
        if (StringUtils.isBlank(mobile) || mobile.length() < 4) return "****";
        return mobile.substring(0, 2) + "******" + mobile.substring(mobile.length() - 2);
    }

    /**
     * Masks Aadhaar number for security logging (e.g. "XXXXXXXX1234").
     *
     * @param aadhaar 12-digit Aadhaar number
     * @return masked Aadhaar string
     */
    private String maskAadhaar(String aadhaar) {
        if (StringUtils.isBlank(aadhaar)) return null;
        String clean = aadhaar.replaceAll("\\s+", "");
        return "XXXXXXXX" + (clean.length() >= 4 ? clean.substring(clean.length() - 4) : "****");
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
        String maskedAadhaar = maskAadhaar(rawAadhaar);
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
}
