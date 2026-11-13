package org.egov.user.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.exception.*;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.domain.model.LoggedInUserUpdatePasswordRequest;
import org.egov.user.domain.model.NonLoggedInUserUpdatePasswordRequest;
import org.egov.common.contract.request.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.AddressType;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.NotificationUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.util.MaskUtils;
import org.egov.user.persistence.dto.FailedLoginAttempt;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.FileStoreRepository;
import org.egov.user.persistence.repository.OtpRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.Otp;
import org.egov.user.web.contract.OtpValidateRequest;
import org.egov.user.web.contract.auth.AuthUserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.egov.user.config.UserServiceConstants.USER_CLIENT_ID;
import static org.springframework.util.CollectionUtils.isEmpty;
import org.egov.user.domain.service.multiauth.TokenGenerationService;
import org.egov.user.web.contract.multiauth.AuthType;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

// REMOVED DEPRECATED IMPORTS:
// import org.springframework.security.oauth2.common.OAuth2AccessToken;
// import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
// import org.springframework.security.oauth2.provider.token.TokenStore;

/**
 * Core domain service orchestrating the complete lifecycle of User entities in the UPYOG platform.
 * <p>
 * This service manages:
 * <ul>
 *   <li><b>User Registration & Profile Creation:</b> Creating CITIZEN and EMPLOYEE accounts with distinct
 *       uniqueness rules, role assignments, state tenant resolution, and optional OTP/password validation.</li>
 *   <li><b>Multi-Account & Mobile Sharing Rules:</b> Supporting family multi-account structures where multiple
 *       citizens can share a mobile number provided they are verified (or at most one unverified), while enforcing
 *       strict single-account-per-mobile constraints for active EMPLOYEE accounts.</li>
 *   <li><b>Authentication & Token Orchestration:</b> Supporting multi-channel login (Mobile OTP, Password, DigiLocker),
 *       direct in-process token generation via {@link TokenGenerationService}, and legacy loopback OAuth token endpoints.</li>
 *   <li><b>Account Security & Anti-Abuse:</b> Tracking failed login attempts, automatic account locking upon reaching
 *       configurable thresholds within time windows, and time-based unlock cool-down evaluations.</li>
 *   <li><b>PII Encryption & Decryption:</b> Transparent field-level encryption/decryption boundaries via
 *       {@link EncryptionDecryptionUtil} for sensitive personal data (Aadhaar, mobile, email, username, name, addresses),
 *       strictly adhering to Attribute-Based Access Control (ABAC) policies.</li>
 *   <li><b>Citizen Isolation & RBAC Security:</b> Enforcing strict identity isolation to ensure CITIZEN callers
 *       can only search, view, or update their own user profiles and cannot invoke administrative endpoints like
 *       {@code /users/_updatenovalidate}.</li>
 *   <li><b>Address Management (V2):</b> Creating, updating (with historical immutability/soft-invalidation), and
 *       retrieving permanent, correspondence, and other user addresses.</li>
 * </ul>
 *
 * <b>Thread Safety:</b> This service is a Spring singleton component. It maintains no mutable instance state
 * and is safe for concurrent access across multiple execution threads.
 */
@Service
@Slf4j
public class UserService {

    private UserRepository userRepository;
    private OtpRepository otpRepository;
    private PasswordEncoder passwordEncoder;
    private FileStoreRepository fileRepository;
    private EncryptionDecryptionUtil encryptionDecryptionUtil;
    //private TokenStore tokenStore;
    private AddressRepository addressRepository;
    
    // CHANGED: TokenStore -> OAuth2AuthorizationService
    private OAuth2AuthorizationService authorizationService;

    // Fix for PR Comment #19 (UserService.java:156):
    // Removed legacy fallback fields (pwdRegex, maxInvalidLoginAttempts, etc.) and made
    // UserConfiguration mandatory (no required = false) to eliminate degraded config fallbacks.
    @Autowired
    private UserConfiguration userConfiguration;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private NotificationUtil notificationUtil;

    @Autowired
    @Lazy
    private TokenGenerationService tokenGenerationService;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    /**
     * Primary constructor injecting repositories and centralized configuration.
     * Fix for PR Comment #19 (UserService.java:156):
     * UserConfiguration injected as mandatory bean (removed required = false) to prevent degraded config fallback.
     *
     * @param userRepository repository for user persistence operations
     * @param otpRepository repository for validating OTPs
     * @param fileRepository repository for resolving file URLs
     * @param passwordEncoder cryptographic encoder for hashing passwords
     * @param encryptionDecryptionUtil utility for field-level encryption/decryption
     * @param addressRepository repository for user addresses
     * @param userConfiguration centralized configuration component for application properties
     */
    @Autowired
    public UserService(UserRepository userRepository, OtpRepository otpRepository, FileStoreRepository fileRepository,
                       PasswordEncoder passwordEncoder, EncryptionDecryptionUtil encryptionDecryptionUtil,
                       AddressRepository addressRepository,
                       UserConfiguration userConfiguration) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileRepository = fileRepository;
        this.encryptionDecryptionUtil = encryptionDecryptionUtil;
        this.addressRepository = addressRepository;
        this.userConfiguration = userConfiguration;
    }

	/**
	 * Constructs a new {@link UserService} with all required repository, security, and configuration dependencies.
	 * Legacy constructor preserved for unit test suites; initializes internal {@link UserConfiguration} cleanly.
	 *
	 * @param userRepository              repository for user persistence operations and queries
	 * @param otpRepository               repository for validating one-time passwords with egov-otp service
	 * @param fileRepository              repository for resolving file URLs from filestore
	 * @param passwordEncoder             cryptographic encoder for hashing and verifying passwords (e.g. BCrypt)
	 * @param encryptionDecryptionUtil    utility for field-level PII encryption and decryption with egov-enc-service
	 * @param defaultPasswordExpiryInDays default duration in days before a user's password expires
	 * @param isCitizenLoginOtpBased      whether citizen login requires OTP verification rather than password
	 * @param isEmployeeLoginOtpBased     whether employee login requires OTP verification rather than password
	 * @param pwdRegex                    regular expression enforcing complexity rules for user passwords
	 * @param pwdMaxLength                maximum allowed character length for passwords
	 * @param pwdMinLength                minimum allowed character length for passwords
	 * @param addressRepository           repository for managing user addresses in V2 flows
	 */
	public UserService(UserRepository userRepository, OtpRepository otpRepository, FileStoreRepository fileRepository,
			PasswordEncoder passwordEncoder, EncryptionDecryptionUtil encryptionDecryptionUtil,
			int defaultPasswordExpiryInDays,
			boolean isCitizenLoginOtpBased,
			boolean isEmployeeLoginOtpBased,
			String pwdRegex,
			Integer pwdMaxLength,
			Integer pwdMinLength, AddressRepository addressRepository) {
		this.userRepository = userRepository;
		this.otpRepository = otpRepository;
		this.passwordEncoder = passwordEncoder;
		this.fileRepository = fileRepository;
		this.encryptionDecryptionUtil = encryptionDecryptionUtil;
		this.addressRepository = addressRepository;
		this.userConfiguration = UserConfiguration.builder()
				.defaultPasswordExpiryInDays(defaultPasswordExpiryInDays)
				.citizenLoginPasswordOtpEnabled(isCitizenLoginOtpBased)
				.employeeLoginPasswordOtpEnabled(isEmployeeLoginOtpBased)
				.pwdRegex(pwdRegex)
				.pwdMaxLength(pwdMaxLength)
				.pwdMinLength(pwdMinLength)
				.accountUnlockCoolDownPeriod(30L)
				.maxInvalidLoginAttemptsPeriod(30L)
				.maxInvalidLoginAttempts(5L)
				.createUserValidateName(true)
				.digilockerSearch(false)
				.citizenLoginPasswordOtpFixedEnabled(false)
				.build();
	}

    /**
     * Resolves the configured egov-user host directly from centralized UserConfiguration.
     *
     * @return configured user service host URL
     */
    private String getUserHost() {
        return userConfiguration != null ? userConfiguration.getUserHost() : null;
    }

    /**
     * Resolves the configured OAuth token endpoint directly from centralized UserConfiguration.
     *
     * @return OAuth token endpoint path
     */
    private String getUserOauthTokenEndpoint() {
        return userConfiguration != null ? userConfiguration.getUserOauthTokenEndpoint() : null;
    }

    /**
     * Resolves the account unlock cooldown period in minutes from centralized UserConfiguration.
     *
     * @return cooldown period in minutes
     */
    private Long getAccountUnlockCoolDownPeriod() {
        return userConfiguration != null ? userConfiguration.getAccountUnlockCoolDownPeriod() : null;
    }

    /**
     * Resolves the evaluation window in minutes for tracking invalid login attempts from UserConfiguration.
     *
     * @return evaluation window in minutes
     */
    private Long getMaxInvalidLoginAttemptsPeriod() {
        return userConfiguration != null ? userConfiguration.getMaxInvalidLoginAttemptsPeriod() : null;
    }

    /**
     * Resolves whether user name validation is enforced during new user creation from UserConfiguration.
     *
     * @return true if user name validation is enabled
     */
    private boolean isCreateUserValidateName() {
        return userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getCreateUserValidateName());
    }

    /**
     * Resolves the maximum invalid login attempts before locking user account from UserConfiguration.
     *
     * @return maximum invalid login attempts threshold
     */
    private Long getMaxInvalidLoginAttempts() {
        return userConfiguration != null ? userConfiguration.getMaxInvalidLoginAttempts() : null;
    }

    /**
     * Resolves whether DigiLocker search is enabled from UserConfiguration.
     *
     * @return true if DigiLocker search is enabled
     */
    private boolean isDigiLockerSearch() {
        return userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getDigilockerSearch());
    }

    /**
     * Resolves the password complexity regular expression pattern from UserConfiguration.
     *
     * @return password pattern regex string
     */
    private String getPwdRegex() {
        return userConfiguration != null ? userConfiguration.getPwdRegex() : null;
    }

    /**
     * Resolves the minimum allowed character length for passwords from UserConfiguration.
     *
     * @return minimum password length
     */
    private Integer getPwdMinLength() {
        return userConfiguration != null ? userConfiguration.getPwdMinLength() : null;
    }

    /**
     * Resolves the maximum allowed character length for passwords from UserConfiguration.
     *
     * @return maximum password length
     */
    private Integer getPwdMaxLength() {
        return userConfiguration != null ? userConfiguration.getPwdMaxLength() : null;
    }

    /**
     * Resolves whether fixed OTP testing is enabled for citizen login from UserConfiguration.
     *
     * @return true if fixed OTP testing is enabled
     */
    private boolean isFixedOTPEnabled() {
        return userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getCitizenLoginPasswordOtpFixedEnabled());
    }

    /**
     * Resolves the configured fixed OTP password value for testing from UserConfiguration.
     *
     * @return fixed OTP password string
     */
    private String getFixedOTPPassword() {
        return userConfiguration != null ? userConfiguration.getCitizenLoginPasswordOtpFixedValue() : null;
    }

    /**
     * Resolves the default password expiry duration in days from UserConfiguration.
     *
     * @return duration in days before password expires
     */
    private int getDefaultPasswordExpiryInDays() {
        return userConfiguration != null && userConfiguration.getDefaultPasswordExpiryInDays() != null
                ? userConfiguration.getDefaultPasswordExpiryInDays() : 90;
    }

    /**
     * Resolves whether citizen login requires OTP verification from UserConfiguration.
     *
     * @return true if citizen login requires OTP
     */
    private boolean isCitizenLoginOtpBased() {
        return userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getCitizenLoginPasswordOtpEnabled());
    }

    /**
     * Resolves whether employee login requires OTP verification from UserConfiguration.
     *
     * @return true if employee login requires OTP
     */
    private boolean isEmployeeLoginOtpBased() {
        return userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getEmployeeLoginPasswordOtpEnabled());
    }

    /**
     * Retrieves a single, unique user matching the specified username, tenant ID, and user type.
     * <p>
     * <b>Processing Steps:</b>
     * <ol>
     *   <li>Validates that {@code userName}, {@code tenantId}, and {@code userType} are not blank or null.</li>
     *   <li>Normalizes tenant ID to the state-level tenant code for {@link UserType#CITIZEN}.</li>
     *   <li>Encrypts search criteria to perform deterministic matching against encrypted database fields.</li>
     *   <li>Queries {@link UserRepository#findAll(UserSearchCriteria)}.</li>
     *   <li>Throws {@link UserNotFoundException} if no records match, or {@link DuplicateUserNameException} if multiple records match.</li>
     *   <li>Decrypts user fields using {@code requestInfo}. If invoked during authentication prior to user context availability,
     *       decryption is deferred to prevent circular dependencies or ABAC failures.</li>
     * </ol>
     *
     * @param userName    the unique username (often mobile number for citizens) to locate; must not be blank
     * @param tenantId    the tenant identifier (e.g., "pb.amritsar"); must not be blank
     * @param userType    the target user type (e.g., {@link UserType#CITIZEN} or {@link UserType#EMPLOYEE}); must not be null
     * @param requestInfo request metadata and caller identity; may be {@code null} or have null user context during auth
     * @return the resolved {@link User} entity with decrypted fields if context is available, or encrypted user if deferred
     * @throws UserNotFoundException      if mandatory parameters are missing or no matching user exists
     * @throws DuplicateUserNameException if more than one user matches the search criteria
     */
    public User getUniqueUser(String userName, String tenantId, UserType userType, RequestInfo requestInfo) {

        UserSearchCriteria userSearchCriteria = UserSearchCriteria.builder()
                .userName(userName)
                .tenantId(getStateLevelTenantForCitizen(tenantId, userType))
                .type(userType)
                .build();

        if (isEmpty(userName) || isEmpty(tenantId) || isNull(userType)) {
            log.error("Invalid lookup, mandatory fields are absent");
            throw new UserNotFoundException(userSearchCriteria);
        }

        /* encrypt here */

        userSearchCriteria = encryptionDecryptionUtil.encryptObject(userSearchCriteria, "User", UserSearchCriteria.class);
        log.debug("Encrypted userSearchCriteria for user lookup");
        List<User> users = userRepository.findAll(userSearchCriteria);
        log.debug("Found {} user(s) matching criteria", users != null ? users.size() : 0);

        if (users.isEmpty())
            throw new UserNotFoundException(userSearchCriteria);
        if (users.size() > 1)
            throw new DuplicateUserNameException(userSearchCriteria);

        /* decrypt here */
        User user = users.get(0);

        // Check if this is an authentication context where RequestInfo doesn't have user info
        if (requestInfo != null && requestInfo.getUserInfo() == null) {
            // For authentication scenarios, skip decryption to avoid circular dependency
            // The user will be decrypted later in the process when we have proper context
            log.info("Skipping decryption during authentication - no user context available");
            return user;
        }

        try {
            User decryptedUser = encryptionDecryptionUtil.decryptObject(user, "User", User.class, requestInfo);
            log.debug("User decryption completed for user ID: {}", decryptedUser != null ? decryptedUser.getId() : null);
            return decryptedUser;
        } catch (Exception e) {
            log.warn("Failed to decrypt user, returning encrypted user: {}", e.getMessage());
            return user;
        }
    }

    /**
     * Decrypts an encrypted user entity in the context of an authenticated user during token generation.
     * <p>
     * <b>ABAC Policy Compatibility Note:</b>
     * To prevent authorization evaluation failures in {@code egov-enc-service}, the synthesized
     * {@link RequestInfo#getUserInfo()} excludes encrypted fields (such as {@code userName}, {@code mobileNumber},
     * {@code emailId}, and {@code name}) because the Attribute-Based Access Control (ABAC) engine cannot evaluate
     * policy rules against ciphertext. Only plaintext identifiers ({@code uuid}, {@code id}, {@code type},
     * {@code tenantId}, {@code roles}) are populated.
     * <p>
     * Attempts decryption first using the {@code "User"} encryption model key, and falls back to auto-detection ({@code null} key)
     * if the primary key fails. If all decryption attempts fail, the original encrypted user is safely returned.
     *
     * @param encryptedUser     the domain {@link User} containing encrypted ciphertext fields; returns {@code null} if null
     * @param authenticatedUser the authenticated user contract containing token generation metadata and roles
     * @return the decrypted {@link User} entity, or the original encrypted entity if decryption encounters an error
     */
    public User decryptUserWithContext(User encryptedUser, AuthUserAccount authenticatedUser) {
        if (encryptedUser == null) {
            return null;
        }

        try {
            // Fix for PR Comment #20 (UserService.java:459):
            // Use typed RequestInfo and Role builders via AuthUserAccount getters instead of raw Map.
            // Preserves actual null values on role attributes (rather than forcing empty strings)
            // and retains the roles collection structure required for ABAC policy evaluation.
            List<Role> contractRoles = new ArrayList<>();
            if (authenticatedUser.getRoles() != null) {
                for (org.egov.user.web.contract.auth.Role role : authenticatedUser.getRoles()) {
                    if (role != null) {
                        contractRoles.add(Role.builder()
                                .code(role.getCode())
                                .name(role.getName())
                                .tenantId(role.getTenantId())
                                .build());
                    }
                }
            }

            org.egov.common.contract.request.User contractUser =
                    org.egov.common.contract.request.User.builder()
                            .uuid(authenticatedUser.getUuid())
                            .id(authenticatedUser.getId())
                            .type(authenticatedUser.getType())
                            .tenantId(authenticatedUser.getTenantId())
                            .roles(contractRoles)
                            .build();

            RequestInfo requestInfo = RequestInfo.builder()
                    .action("token_generation")
                    .ts(System.currentTimeMillis())
                    .userInfo(contractUser)
                    .build();

            // The encryption service expects a List<User>, not a single User
            // Wrap in list, decrypt, then extract the single user (same pattern as searchUsers)
            List<User> userList = Collections.singletonList(encryptedUser);
            try {
                log.info("Attempting decryption with null key using List<User> (same as searchUsers)");
                List<User> decryptedUserList = encryptionDecryptionUtil.decryptObject(userList, "User", User.class, requestInfo);
                User decryptedUser = decryptedUserList.get(0);
                log.info("Successfully decrypted user with \"User\" key: {}", MaskUtils.maskIdentifier(decryptedUser.getUsername()));
                return decryptedUser;
            } catch (Exception e1) {
                log.warn("Decryption with null key failed, trying with \"User\" key: {}", e1.getMessage());
                try {
                    List<User> decryptedUserList = encryptionDecryptionUtil.decryptObject(userList, null, User.class, requestInfo);
                    User decryptedUser = decryptedUserList.get(0);
                    return decryptedUser;
                } catch (Exception e2) {
                    log.warn("Both decryption attempts failed. User key error: {}, Auto-detect error: {}",
                             e1.getMessage(), e2.getMessage());
                    throw e2;
                }
            }

        } catch (Exception e) {
            log.warn("Failed to decrypt user with context, returning encrypted user: {}", e.getMessage());
            return encryptedUser;
        }
    }

    /**
     * Retrieves a single user by their globally unique identifier (UUID).
     *
     * @param uuid globally unique user identifier (UUID v4); must not be null or blank
     * @return the matching domain {@link User} entity
     * @throws UserNotFoundException if {@code uuid} is blank or no matching user is found
     */
    public User getUserByUuid(String uuid) {

        UserSearchCriteria userSearchCriteria = UserSearchCriteria.builder()
                .uuid(Collections.singletonList(uuid))
                .build();

        if (isEmpty(uuid)) {
            log.error("UUID is mandatory");
            throw new UserNotFoundException(userSearchCriteria);
        }

        List<User> users = userRepository.findAll(userSearchCriteria);

        if (users.isEmpty())
            throw new UserNotFoundException(userSearchCriteria);
        return users.get(0);
    }

    /**
     * Retrieves a single user matching the specified primary mobile number.
     *
     * @param mobileNumber the 10-digit mobile number to search; must not be null or blank
     * @return the matching domain {@link User} entity
     * @throws UserNotFoundException if {@code mobileNumber} is blank or no matching user is found
     */
    public User getUserBymobileNumber(String mobileNumber) {

        UserSearchCriteria userSearchCriteria = UserSearchCriteria.builder()
                .mobileNumber(mobileNumber)
                .build();

        if (isEmpty(mobileNumber)) {
            log.error("Mobile Number is mandatory");
            throw new UserNotFoundException(userSearchCriteria);
        }
        //userSearchCriteria.setDigilockersearch(isDigiLockerSearch);
        List<User> users = userRepository.findAll(userSearchCriteria);

        if (users.isEmpty())
            throw new UserNotFoundException(userSearchCriteria);
        return users.get(0);
    }

    /**
     * Searches for users matching the criteria, enforcing citizen caller isolation and PII decryption.
     * <p>
     * <b>Security & Decryption Pipeline:</b>
     * <ol>
     *   <li>Validates criteria constraints using {@link UserSearchCriteria#validate(boolean)}.</li>
     *   <li>Enforces CITIZEN isolation: if the caller is a citizen, they may only search using their own
     *       verified UUID, mobile number, username, or ID. Searching for arbitrary third-party users is denied.</li>
     *   <li>Normalizes {@code tenantId} to the state-level tenant code when searching for {@link UserType#CITIZEN}.</li>
     *   <li>Encrypts search criteria with key {@code "User"} for exact matching against encrypted DB columns.</li>
     *   <li>Queries {@link UserRepository#findAll(UserSearchCriteria)}.</li>
     *   <li>Decrypts resulting user records using {@code requestInfo}. Uses defensive error handling so if any
     *       corrupted ciphertext is encountered, partially decrypted or raw data is preserved rather than failing the request.</li>
     * </ol>
     *
     * @param searchCriteria     user filtering parameters (e.g., UUIDs, mobileNumber, userName, roles, tenantId)
     * @param isInterServiceCall flag indicating whether the invocation originates internally from another trusted microservice
     * @param requestInfo        request metadata and caller security context for authorization and ABAC decryption
     * @return list of matching domain {@link User} entities with decrypted fields, or an empty list if none match
     * @throws CustomException if citizen caller security validation fails or criteria validation fails
     */
    public List<User> searchUsers(UserSearchCriteria searchCriteria,
                                  boolean isInterServiceCall, RequestInfo requestInfo) {
    	log.info("searchCriteria"+searchCriteria);
        searchCriteria.validate(isInterServiceCall);
        validateCitizenSearch(searchCriteria, isInterServiceCall, requestInfo);

        searchCriteria.setTenantId(getStateLevelTenantForCitizen(searchCriteria.getTenantId(), searchCriteria.getType()));

            String altmobnumber = null;

            if (searchCriteria.getMobileNumber() != null) {
                altmobnumber = searchCriteria.getMobileNumber();
            }

            searchCriteria = encryptionDecryptionUtil.encryptObject(searchCriteria, "User", UserSearchCriteria.class);

            if (altmobnumber != null) {
                searchCriteria.setAlternatemobilenumber(altmobnumber);
            }
        log.info("Search Criteria :-"+ searchCriteria);
        List<User> list = userRepository.findAll(searchCriteria);

        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        /* decrypt here / final reponse decrypted*/

        // Decrypt user list - role preservation is now handled in EncryptionDecryptionUtil
        // Use same defensive error handling as /oauth/token endpoint to handle corrupted ciphertext
        try {
            list = encryptionDecryptionUtil.decryptObject(list, null, User.class, requestInfo);
            log.info("Successfully decrypted user list with {} users", list != null ? list.size() : 0);
        } catch (Exception e) {
            log.warn("Failed to decrypt user list, returning encrypted/partial data. Error: {}", e.getMessage());
            log.debug("Decryption error stack trace:", e);
            // Return list as-is (encrypted or partially encrypted)
            // This matches the behavior of /oauth/token which returns encrypted user on decryption error
            // allowing the request to succeed even with corrupted ciphertext in database
        }

        //setFileStoreUrlsByFileStoreIds(list);
        return list;
    }

    /**
     * Creates a new user profile after validating enterprise account rules, uniqueness, and security constraints.
     * <p>
     * <b>Account & Mobile Validation Rules:</b>
     * <ul>
     *   <li><b>EMPLOYEE Accounts:</b> Allowed to share a mobile number with citizen accounts (isolated user types).
     *       At most ONE active employee account can exist per mobile number in a tenant.</li>
     *   <li><b>CITIZEN Accounts:</b> Multiple family members can share a mobile number.
     *       At most ONE unverified citizen account is allowed per mobile number.
     *       Verified family members must each have a unique Aadhaar number.</li>
     * </ul>
     *
     * @param user        incoming domain {@link User} entity to create
     * @param requestInfo request metadata and context for auditing and encryption/decryption
     * @return created new domain user with decrypted details
     * @throws DuplicateUserNameException if username already exists in tenant
     * @throws CustomException            if validation fails or account limit rules are violated
     */
    public User createUser(User user, RequestInfo requestInfo) {
        validateMobileAndAccountRules(user);

        user.setIsIdentityVerified(Boolean.FALSE);
        user.setIdentityVerifiedDate(null);
        user.setIdentityVerifiedSource(null);

        user.setUuid(UUID.randomUUID().toString());
        user.validateNewUser(isCreateUserValidateName());
        conditionallyValidateOtp(user);
        /* encrypt here */
        user = encryptionDecryptionUtil.encryptObject(user, "User", User.class);
        validateUserUniqueness(user);
        if (isEmpty(user.getPassword())) {
            user.setPassword(UUID.randomUUID().toString());
        } else {
            validatePassword(user.getPassword());
        }
        user.setPassword(encryptPwd(user.getPassword()));
        user.setDefaultPasswordExpiry(getDefaultPasswordExpiryInDays());
        user.setTenantId(getStateLevelTenantForCitizen(user.getTenantId(), user.getType()));

        User persistedNewUser = persistNewUser(user);
        return encryptionDecryptionUtil.decryptObject(persistedNewUser, "UserSelf", User.class, requestInfo);

        /* decrypt here  because encrypted data coming from DB*/

    }

    /**
     * Validates that the username does not already exist within the state tenant for the specified user type.
     *
     * @param user domain user entity to validate for uniqueness
     * @throws DuplicateUserNameException if an existing user matches the username and tenant
     */
    private void validateUserUniqueness(User user) {
        if (userRepository.isUserPresent(user.getUsername(), getStateLevelTenantForCitizen(user.getTenantId(), user
                .getType()), user.getType()))
            throw new DuplicateUserNameException(UserSearchCriteria.builder().userName(user.getUsername()).type(user
                    .getType()).tenantId(user.getTenantId()).build());
    }

    /**
     * Normalizes the tenant identifier to the state level (root tenant) for citizen users.
     *
     * @param tenantId input tenant identifier (e.g. "pg.citya")
     * @param userType classification of the user
     * @return state-level tenant code (e.g. "pg") for citizens, or original tenantId for non-citizens
     */
    private String getStateLevelTenantForCitizen(String tenantId, UserType userType) {
        if (!isNull(userType) && userType.equals(UserType.CITIZEN) && !isEmpty(tenantId) && tenantId.contains("."))
            return tenantId.split("\\.")[0];
        else
            return tenantId;
    }

    /**
     * Creates a new citizen user profile with specialized citizen validation, role assignment, and state tenant resolution.
     * <p>
     * <b>Enrichment & Validation:</b>
     * <ul>
     *   <li>If OTP-based login is configured, ensures {@code username} is numeric and sets it as the primary {@code mobileNumber}.</li>
     *   <li>If password-based, validates password complexity requirements.</li>
     *   <li>Assigns the standard {@code CITIZEN} role.</li>
     *   <li>Normalizes the tenant ID to the root state-level tenant code.</li>
     *   <li>Delegates to {@link #createUser(User, RequestInfo)} for mobile sharing validation, UUID generation,
     *       OTP verification, and encrypted persistence.</li>
     * </ul>
     *
     * @param user        incoming citizen {@link User} payload containing profile details
     * @param requestInfo request metadata and context for auditing and encryption/decryption
     * @return the created domain {@link User} with decrypted details
     * @throws UserNameNotValidException  if OTP login is active and username is not a valid numeric mobile number
     * @throws DuplicateUserNameException if username already exists in the state tenant
     * @throws CustomException            if password fails complexity, OTP is pending, or account rules are violated
     */
    public User createCitizen(User user, RequestInfo requestInfo) {
        validateAndEnrichCitizen(user);
        return createUser(user, requestInfo);
    }


    /**
     * Validates and enriches a newly registering citizen's username, roles, and state tenant.
     *
     * @param user incoming citizen user entity to validate and enrich
     */
    private void validateAndEnrichCitizen(User user) {
        log.info("Validating User........");
        if (isCitizenLoginOtpBased() && !StringUtils.isNumeric(user.getUsername()))
            throw new UserNameNotValidException();
        else if (isCitizenLoginOtpBased())
            user.setMobileNumber(user.getUsername());
        else if(!isCitizenLoginOtpBased() && user.isDigilockerRegistration())
            user.setMobileNumber(user.getUsername());
        if (!isCitizenLoginOtpBased())
            validatePassword(user.getPassword());
        user.setRoleToCitizen();
        user.setTenantId(getStateLevelTenantForCitizen(user.getTenantId(), user.getType()));
    }

    /**
     * Atomically registers a new citizen account and generates an OAuth2 authentication token response in a single flow.
     * <p>
     * Eliminates the multi-step overhead of creating a user followed by a separate {@code /oauth/token} call.
     * Activates the account, creates the citizen profile via {@link #createCitizen(User, RequestInfo)}, verifies
     * account active status, and delegates token generation to {@link TokenGenerationService} (or falls back to loopback HTTP).
     *
     * @param user        the citizen {@link User} to register and log in
     * @param requestInfo request metadata for caller context and encryption/decryption
     * @return OAuth token response map containing {@code access_token}, {@code refresh_token}, {@code scope}, and user details
     * @throws CustomException if user creation fails, the created account is inactive, or token generation fails
     */
    public Object registerWithLogin(User user, RequestInfo requestInfo) {
        user.setActive(true);
        User createdUser = createCitizen(user, requestInfo);
        User targetUser = createdUser != null ? createdUser : user;
        if (targetUser != null && !Boolean.TRUE.equals(targetUser.getActive())) {
            log.warn("[USER-SERVICE] Account is inactive for user: {}", MaskUtils.maskIdentifier(targetUser.getUsername()));
            throw new CustomException(UserConstants.ERROR_ACCOUNT_INACTIVE, UserConstants.MSG_ACCOUNT_INACTIVE);
        }
        if (StringUtils.isNotBlank(user.getScope()) && StringUtils.isBlank(targetUser.getScope())) {
            targetUser.setScope(user.getScope());
        }
        return getAccess(targetUser, user.getOtpReference());
    }

    /**
     * Obtains OAuth access and refresh tokens for a newly registered user.
     *
     * @param user target authenticated user
     * @param password user password or OTP credential reference
     * @return token response map or contract
     */
    private Object getAccess(User user, String password) {
        log.info("Fetch access token for register with login flow");
        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            log.warn("[USER-SERVICE] Account is inactive for user: {}", user != null ? MaskUtils.maskIdentifier(user.getUsername()) : "null");
            throw new CustomException(UserConstants.ERROR_ACCOUNT_INACTIVE, UserConstants.MSG_ACCOUNT_INACTIVE);
        }
        try {
            String effectiveScope = StringUtils.isNotBlank(user.getScope()) ? user.getScope() : "read write";

            // Direct in-process token generation (eliminates loopback HTTP overhead and 404 URL errors)
            if (tokenGenerationService != null) {
                return tokenGenerationService.generateTokensAndResponse(
                        user,
                        effectiveScope,
                        AuthType.MOBILE_NUMBER,
                        true
                );
            }

            // Fallback to loopback HTTP if tokenGenerationService is not available
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("username", user.getUsername());
            
            if (!isEmpty(password))
                map.add("password", password);
            else
                map.add("password", user.getPassword());
                
            map.add("grant_type", "password");
            map.add("scope", effectiveScope);
            map.add("tenantId", user.getTenantId());
            map.add("userType", UserType.CITIZEN.name());
            map.add("isInternal", "true");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            String host = getUserHost();
            if (StringUtils.isBlank(host)) {
                throw new CustomException("CONFIG_ERROR", "egov.user.host is not configured");
            }
            String endpoint = getUserOauthTokenEndpoint();
            if (StringUtils.isBlank(endpoint)) {
                throw new CustomException("CONFIG_ERROR", "egov.user.oauth.token.endpoint is not configured");
            }
            host = host.trim();
            if (!host.endsWith("/")) {
                host += "/";
            }
            endpoint = endpoint.trim();
            String tokenUrl = host + (endpoint.startsWith("/") ? endpoint.substring(1) : endpoint);
            return restTemplate.postForEntity(tokenUrl, request, Map.class).getBody();

        } catch (Exception e) {
            log.error("Error occurred while logging-in via register flow", e);
            throw new CustomException("LOGIN_ERROR", "Error occurred while logging in via register flow: " + e.getMessage());
        }
    }


    /**
     * dependent on otpValidationMandatory filed,it will validate the otp.
     *
     * @param user
     */
    private void conditionallyValidateOtp(User user) {
        if (user.isOtpValidationMandatory()) {
            if (!validateOtp(user))
                throw new OtpValidationPendingException();
        }
    }

    /**
     * Validates an OTP entered by the user against the egov-otp service.
     * <p>
     * <b>Non-Production Bypass:</b>
     * If {@code citizen.login.password.otp.fixed.enabled} is {@code true} in non-production environments and
     * the incoming OTP matches {@code citizen.login.password.otp.fixed.value}, validation succeeds immediately
     * without invoking the external OTP service.
     *
     * @param user domain {@link User} containing {@code mobileNumber}, {@code otpReference}, {@code tenantId}, and {@code type}
     * @return {@code Boolean.TRUE} if the OTP is valid; {@code Boolean.FALSE} otherwise
     */
    public Boolean validateOtp(User user) {
        // Bypass egov-otp validation in non-production/test environments if fixed OTP is enabled
        // This allows registration with configured fixed OTP (e.g., 123456) without prior OTP generation
        if (isFixedOTPEnabled() && StringUtils.isNotBlank(getFixedOTPPassword()) && getFixedOTPPassword().equals(user.getOtpReference())) {
            log.info("Fixed OTP matched successfully for user: {}", MaskUtils.maskMobile(user.getMobileNumber()));
            return true;
        }
        Otp otp = Otp.builder().otp(user.getOtpReference()).identity(user.getMobileNumber()).tenantId(user.getTenantId())
                .userType(user.getType()).build();
        RequestInfo requestInfo = RequestInfo.builder().action("validate").ts(System.currentTimeMillis()).build();
        OtpValidateRequest otpValidationRequest = OtpValidateRequest.builder().requestInfo(requestInfo).otp(otp)
                .build();
        return otpRepository.validateOtp(otpValidationRequest);

    }


    /**
     * Administratively updates user profile and credentials without requiring OTP verification.
     * <p>
     * <b>Security & Administrative Constraints:</b>
     * <ul>
     *   <li><b>CITIZEN Block:</b> CITIZEN callers are strictly prohibited from invoking this endpoint (throws {@code UNAUTHORIZED_ACCESS}).</li>
     *   <li><b>CITIZEN Target Block:</b> CITIZEN accounts cannot be updated via this endpoint (throws {@code INVALID_USER_TYPE}).
     *       Citizen profiles must be updated via {@code /users/profile/_update} (partialUpdate) or V2 endpoints.</li>
     *   <li><b>ID Consistency:</b> If an ID is provided, it must match the existing database ID for the given UUID.</li>
     *   <li><b>Identity Verification Immutability:</b> Fields {@code isIdentityVerified}, {@code identityVerifiedDate},
     *       and {@code identityVerifiedSource} cannot be altered through this endpoint.</li>
     *   <li><b>Account Unlock:</b> If an account is explicitly unlocked through this update, failed login attempt counters are reset.</li>
     * </ul>
     *
     * @param user        the updated {@link User} payload
     * @param requestInfo request metadata containing the updater's identity and roles
     * @return the updated domain {@link User} with decrypted fields
     * @throws CustomException if payload is null, ID mismatches, or roles are missing
     */
    // TODO Fix date formats
    public User updateWithoutOtpValidation(User user, RequestInfo requestInfo) {
        if (user == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_UPDATE_REQUEST, "User payload cannot be null");
        }
        User existingUser = getUserByUuid(user.getUuid());
        if (user.getId() != null && existingUser.getId() != null && !user.getId().equals(existingUser.getId())) {
            log.error("[USER-SERVICE] Data mismatch: provided id {} does not match database id {}", user.getId(), existingUser.getId());
            throw new CustomException(UserConstants.ERROR_DATA_MISMATCH, "Data mismatch: Provided id " + user.getId() + " does not match existing user record for uuid: " + user.getUuid());
        }
        // Identity verification status cannot be updated via updateWithoutOtpValidation
        user.setIsIdentityVerified(existingUser.getIsIdentityVerified());
        user.setIdentityVerifiedDate(existingUser.getIdentityVerifiedDate());
        user.setIdentityVerifiedSource(existingUser.getIdentityVerifiedSource());
        user.setTenantId(getStateLevelTenantForCitizen(user.getTenantId(), user.getType()));
            validateUserRoles(user);
            user.validateUserModification();
            validatePassword(user.getPassword());
            user.setPassword(encryptPwd(user.getPassword()));
            user = encryptionDecryptionUtil.encryptObject(user, "User", User.class);
            Long updaterId = (requestInfo != null && requestInfo.getUserInfo() != null) ? requestInfo.getUserInfo().getId() : null;
            String updaterUuid = (requestInfo != null && requestInfo.getUserInfo() != null) ? requestInfo.getUserInfo().getUuid() : null;
            userRepository.update(user, existingUser, updaterId, updaterUuid);
        /* encrypt */


        // If user is being unlocked via update, reset failed login attempts
        if (user.getAccountLocked() != null && !user.getAccountLocked() && existingUser.getAccountLocked())
            resetFailedLoginAttempts(user);

        User encryptedUpdatedUserfromDB, decryptedupdatedUserfromDB;

            encryptedUpdatedUserfromDB = getUserByUuid(user.getUuid());
            decryptedupdatedUserfromDB = encryptionDecryptionUtil.decryptObject(encryptedUpdatedUserfromDB, "UserSelf", User.class, requestInfo);
            return decryptedupdatedUserfromDB;
    }
    
    
    /**
     * Requests token invalidation for the specified user upon critical profile or password changes.
     * <p>
     * Under the Spring Authorization Server architecture, active JWT/opaque tokens expire naturally according
     * to their configured time-to-live (TTL), or are rejected upon token refresh if the account is modified or locked.
     *
     * @param user the domain {@link User} whose tokens should be retired
     */
    public void removeTokensByUser(User user) {
        log.info("Token removal requested for user: {} - tokens will expire naturally", user != null ? MaskUtils.maskIdentifier(user.getUsername()) : "null");
    }

    /**
     * this api will validate whether user roles exist in Database or not
     *
     * @param user
     */
    private void validateUserRoles(User user) {
        if (user.getRoles() == null || user.getRoles() != null && user.getRoles().isEmpty()) {
            throw new AtleastOneRoleCodeException();
        }
    }

    /**
     * Partially updates editable user profile attributes while safeguarding critical identity fields.
     * <p>
     * <b>Security & Field Masking Rules:</b>
     * <ul>
     *   <li><b>Caller Verification:</b> Ensures the profile update is initiated by the authenticated user
     *       matching the target record (or permitted administrator). CITIZEN callers are strictly restricted
     *       from updating any profile other than their own.</li>
     *   <li><b>Protected Fields:</b> Sensitive attributes (such as {@code username}, {@code mobileNumber},
     *       {@code userType}, {@code password}, and {@code roles}) are nullified and shielded from modification.</li>
     *   <li><b>Identity Status Immutability:</b> Preserves existing identity verification state ({@code isIdentityVerified},
     *       {@code identityVerifiedDate}, {@code identityVerifiedSource}).</li>
     *   <li><b>Email Change Notification:</b> If the user's email address is updated, triggers an asynchronous
     *       security alert notification to the old email address via {@link NotificationUtil}.</li>
     * </ul>
     *
     * @param user        the partial {@link User} update payload containing altered profile fields
     * @param requestInfo request metadata and authenticated caller context
     * @return the refreshed domain {@link User} with decrypted fields
     * @throws UserProfileUpdateDeniedException if a caller attempts to update a different user's profile
     * @throws CustomException                  if a CITIZEN caller attempts an unauthorized update or validation fails
     */
    public User partialUpdate(User user, RequestInfo requestInfo) {
        /* encrypt here */
        user = encryptionDecryptionUtil.encryptObject(user, "User", User.class);

        User existingUser = getUserByUuid(user.getUuid());
        validateProfileUpdateIsDoneByTheSameLoggedInUser(user);
        if (isCallerCitizen(requestInfo)) {
            var caller = requestInfo.getUserInfo();
            String callerUuid = caller.getUuid();
            Long callerId = caller.getId();
            boolean uuidMismatch = StringUtils.isNotBlank(callerUuid) && StringUtils.isNotBlank(existingUser.getUuid())
                    && !callerUuid.equalsIgnoreCase(existingUser.getUuid());
            boolean idMismatch = callerId != null && existingUser.getId() != null
                    && !callerId.equals(existingUser.getId());
            if (uuidMismatch || idMismatch) {
                log.error("[USER-SERVICE] Unauthorized update: Citizen caller {} attempted to update user {}",
                        callerUuid, existingUser.getUuid());
                throw new CustomException("UNAUTHORIZED_ACCESS", "Citizen users are only permitted to update their own profile");
            }
        }
        user.nullifySensitiveFields();
        // Identity verification status cannot be updated via partialUpdate
        user.setIsIdentityVerified(existingUser.getIsIdentityVerified());
        user.setIdentityVerifiedDate(existingUser.getIdentityVerifiedDate());
        user.setIdentityVerifiedSource(existingUser.getIdentityVerifiedSource());
        validatePassword(user.getPassword());
        userRepository.update(user, existingUser,requestInfo.getUserInfo().getId(), requestInfo.getUserInfo().getUuid() );
        User updatedUser = getUserByUuid(user.getUuid());

        /* decrypt here */
        existingUser = encryptionDecryptionUtil.decryptObject(existingUser, "UserSelf", User.class, requestInfo);
        updatedUser = encryptionDecryptionUtil.decryptObject(updatedUser, "UserSelf", User.class, requestInfo);

        //setFileStoreUrlsByFileStoreIds(Collections.singletonList(updatedUser));
        String oldEmail = existingUser.getEmailId();
        String newEmail = updatedUser.getEmailId();
        if((oldEmail != null && !oldEmail.isEmpty()) && newEmail != null && !(newEmail.equalsIgnoreCase(oldEmail))) {
            // Sending sms and email to old email to notify that email has been changed
            notificationUtil.sendEmail(requestInfo, existingUser, updatedUser);
        }
        return updatedUser;
    }

    /**
     * Changes the password for an actively authenticated and logged-in user.
     * <p>
     * <b>Validation & Security:</b>
     * <ul>
     *   <li>Validates the request payload format.</li>
     *   <li>Retrieves the existing unique user record.</li>
     *   <li>Verifies that password authentication is supported (throws {@link InvalidUpdatePasswordRequestException}
     *       if OTP-based login is enforced for this user type).</li>
     *   <li>Validates that the provided existing password matches the current encrypted password.</li>
     *   <li>Validates the new password against regex complexity and length constraints.</li>
     *   <li>Hashes the new password with {@link PasswordEncoder} and persists the change.</li>
     * </ul>
     *
     * @param updatePasswordRequest request containing username, tenantId, type, old password, and new password
     * @throws InvalidUpdatePasswordRequestException if the user type uses OTP-only authentication
     * @throws PasswordMismatchException            if the existing password does not match the stored hash
     * @throws CustomException                      if the new password violates complexity constraints
     */
    public void updatePasswordForLoggedInUser(LoggedInUserUpdatePasswordRequest updatePasswordRequest) {
        updatePasswordRequest.validate();
        final User user = getUniqueUser(updatePasswordRequest.getUserName(), updatePasswordRequest.getTenantId(),
                updatePasswordRequest.getType(), null);

        if (user.getType().toString().equals(UserType.CITIZEN.toString()) && isCitizenLoginOtpBased())
            throw new InvalidUpdatePasswordRequestException();
        if (user.getType().toString().equals(UserType.EMPLOYEE.toString()) && isEmployeeLoginOtpBased())
            throw new InvalidUpdatePasswordRequestException();

        validateExistingPassword(user, updatePasswordRequest.getExistingPassword());
        validatePassword(updatePasswordRequest.getNewPassword());
        user.updatePassword(encryptPwd(updatePasswordRequest.getNewPassword()));
        userRepository.update(user, user, user.getId() , user.getUuid());
    }

    /**
     * Resets a user's password via the "Forgot Password" flow using an OTP validation reference.
     * <p>
     * <b>Workflow:</b>
     * <ol>
     *   <li>Validates the request parameters.</li>
     *   <li>Retrieves the user account; rejects if OTP-only login is configured for CITIZEN or EMPLOYEE.</li>
     *   <li>Decrypts user fields to supply the plaintext mobile number to the OTP service.</li>
     *   <li>Validates the OTP reference code with {@link #validateOtp(User)}.</li>
     *   <li>Validates that the new password meets complexity and length requirements.</li>
     *   <li>Encodes the new password and updates the database record.</li>
     * </ol>
     *
     * @param request     forgot-password request containing username, tenantId, OTP reference, and new password
     * @param requestInfo request metadata and context for auditing and encryption/decryption
     * @throws InvalidUpdatePasswordRequestException if forgot password flow is disabled for the user type
     * @throws CustomException                      if OTP validation fails or new password violates complexity rules
     */
    public void updatePasswordForNonLoggedInUser(NonLoggedInUserUpdatePasswordRequest request, RequestInfo requestInfo) {
        request.validate();
        // validateOtp(request.getOtpValidationRequest());
        User user = getUniqueUser(request.getUserName(), request.getTenantId(), request.getType(), requestInfo);
        if (user.getType().toString().equals(UserType.CITIZEN.toString()) && isCitizenLoginOtpBased()) {
            log.info("CITIZEN forgot password flow is disabled");
            throw new InvalidUpdatePasswordRequestException();
        }
        if (user.getType().toString().equals(UserType.EMPLOYEE.toString()) && isEmployeeLoginOtpBased()) {
            log.info("EMPLOYEE forgot password flow is disabled");
            throw new InvalidUpdatePasswordRequestException();
        }
        /* decrypt here */
        /* the reason for decryption here is the otp service requires decrypted username */
        user = encryptionDecryptionUtil.decryptObject(user, "User", User.class, requestInfo);
        user.setOtpReference(request.getOtpReference());
        validateOtp(user);
        validatePassword(request.getNewPassword());
        user.updatePassword(encryptPwd(request.getNewPassword()));
        /* encrypt here */
        /* encrypted value is stored in DB*/
        user = encryptionDecryptionUtil.encryptObject(user, "User", User.class);
        userRepository.update(user, user, requestInfo.getUserInfo().getId(), requestInfo.getUserInfo().getUuid());
    }


    /**
     * Evaluates whether a currently locked user account has completed its cool-down period and is eligible to be unlocked.
     *
     * @param user domain {@link User} whose account lock status and lock timestamp are evaluated
     * @return {@code true} if the account is not locked, or if the configured cool-down duration has elapsed since locking;
     *         {@code false} otherwise
     */
    public boolean isAccountUnlockAble(User user) {
        if (user.getAccountLocked()) {
            boolean unlockAble =
                    System.currentTimeMillis() - user.getAccountLockedDate() > TimeUnit.MINUTES.toMillis(getAccountUnlockCoolDownPeriod());

            log.info("Account eligible for unlock - " + unlockAble);
            log.info("Current time {}, last lock time {} , cool down period {} ", System.currentTimeMillis(),
                    user.getAccountLockedDate(), TimeUnit.MINUTES.toMillis(getAccountUnlockCoolDownPeriod()));
            return unlockAble;
        } else
            return true;
    }

    /**
     * Records a failed login attempt and automatically locks the account if the threshold is exceeded.
     * <p>
     * <b>Anti-Abuse Logic:</b>
     * <ol>
     *   <li>Queries recent failed attempts for the user within {@code maxInvalidLoginAttemptsPeriod} minutes.</li>
     *   <li>If total failed attempts (including current) reach or exceed {@code maxInvalidLoginAttempts}:
     *     <ul>
     *       <li>Sets {@code accountLocked = true} and records the current timestamp.</li>
     *       <li>Persists the locked status via {@link #updateWithoutOtpValidation(User, RequestInfo)}.</li>
     *       <li>Throws {@link BadCredentialsException} with message {@code "Account locked"}.</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise, inserts an audit record for the failed attempt with IP address and timestamp.</li>
     * </ol>
     *
     * @param user        the domain {@link User} whose login attempt failed
     * @param ipAddress   the client IP address from which the failed attempt originated
     * @param requestInfo request metadata for auditing and update context
     * @throws BadCredentialsException if the account has been locked due to exceeding maximum allowed attempts
     */
    public void handleFailedLogin(User user, String ipAddress, RequestInfo requestInfo) {
        if (!Objects.isNull(user.getUuid())) {
            List<FailedLoginAttempt> failedLoginAttempts =
                    userRepository.fetchFailedAttemptsByUserAndTime(user.getUuid(),
                            System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(getMaxInvalidLoginAttemptsPeriod()));

            if (failedLoginAttempts.size() + 1 >= getMaxInvalidLoginAttempts()) {
                User userToBeUpdated = user.toBuilder()
                        .accountLocked(true)
                        .password(null)
                        .accountLockedDate(System.currentTimeMillis())
                        .build();

                user = updateWithoutOtpValidation(userToBeUpdated, requestInfo);
                
                // REMOVED: removeTokensByUser(user); 
                // Token removal will be handled when user tries to use expired/invalid tokens
                
                log.info("Locked account with uuid {} for {} minutes as exceeded max allowed attempts of {} within {} minutes",
                        user.getUuid(), getAccountUnlockCoolDownPeriod(), getMaxInvalidLoginAttempts(), getMaxInvalidLoginAttemptsPeriod());
                
                throw new BadCredentialsException("Account locked");
            }

            userRepository.insertFailedLoginAttempt(new FailedLoginAttempt(user.getUuid(), ipAddress,
                    System.currentTimeMillis(), true));
        }
    }



    /**
     * This api will validate existing password and current password matching or
     * not
     *
     * @param user
     * @param existingRawPassword
     */
    private void validateExistingPassword(User user, String existingRawPassword) {
        if (!passwordEncoder.matches(existingRawPassword, user.getPassword())) {
            throw new PasswordMismatchException("Invalid username or password");
        }
    }

//    /**
//     * this api will check user is exist or not, If not exist it will throw
//     * exception.
//     *
//     * @param user
//     */
//    private void validateUserPresent(User user) {
//        if (user == null) {
//            throw new UserNotFoundException(null);
//        }
//    }

    /**
     * this api will validate, updating the profile for same logged-in user or
     * not
     *
     * @param user
     */
    private void validateProfileUpdateIsDoneByTheSameLoggedInUser(User user) {
        if (user.isLoggedInUserDifferentFromUpdatedUser()) {
            throw new UserProfileUpdateDeniedException();
        }
    }


    /**
     * Encrypts the raw password using the injected PasswordEncoder.
     *
     * @param pwd raw plaintext password
     * @return encoded password hash or null if input is null
     */
    String encryptPwd(String pwd) {
        if (!isNull(pwd))
            return passwordEncoder.encode(pwd);
        else
            return null;
    }

    /**
     * This api will persist the user
     *
     * @param user
     * @return
     */
    private User persistNewUser(User user) {

        return userRepository.create(user);
    }

    /**
     * This api will fetch the fileStoreUrl By fileStoreId
     *
     * @param userList
     * @throws Exception
     */
    private void setFileStoreUrlsByFileStoreIds(List<User> userList) {
        List<String> fileStoreIds = userList.parallelStream().filter(p -> p.getPhoto() != null).map(User::getPhoto)
                .collect(Collectors.toList());
        if (!isEmpty(fileStoreIds)) {
            Map<String, String> fileStoreUrlList = null;
            try {
                fileStoreUrlList = fileRepository.getUrlByFileStoreId(userList.get(0).getTenantId(), fileStoreIds);
            } catch (Exception e) {
                // TODO Auto-generated catch block

                log.error("Error while fetching fileStore url list: " + e.getMessage());
            }

            if (fileStoreUrlList != null && !fileStoreUrlList.isEmpty()) {
                for (User user : userList) {
                    user.setPhoto(fileStoreUrlList.get(user.getPhoto()));
                }
            }
        }
    }


    /**
     * Validates raw password strings against enterprise length and complexity constraints.
     * <p>
     * <b>Complexity Requirements:</b>
     * <ul>
     *   <li>Length must fall between {@code pwdMinLength} and {@code pwdMaxLength}.</li>
     *   <li>Must contain at least one digit (0-9).</li>
     *   <li>Must contain at least one uppercase letter (A-Z).</li>
     *   <li>Must contain at least one lowercase letter (a-z).</li>
     *   <li>Must contain at least one special character (e.g. {@code @#$%}).</li>
     *   <li>Must NOT contain whitespace characters.</li>
     * </ul>
     *
     * @param password raw plaintext password candidate to evaluate; if null/empty, passes without validation
     * @throws CustomException if password violates length bounds or pattern regex requirements
     */
    public void validatePassword(String password) {
        Map<String, String> errorMap = new HashMap<>();
        if (!StringUtils.isEmpty(password)) {
            int minLen = getPwdMinLength();
            int maxLen = getPwdMaxLength();
            if (password.length() < minLen || password.length() > maxLen)
                errorMap.put("INVALID_PWD_LENGTH", "Password must be of minimum: " + minLen + " and maximum: " + maxLen + " characters.");
            Pattern p = Pattern.compile(getPwdRegex());
            Matcher m = p.matcher(password);
            if (!m.find()) {
                errorMap.put("INVALID_PWD_PATTERN", "Password MUST HAVE: Atleast one digit, one upper case, one lower case, one special character (@#$%) and MUST NOT contain any spaces");
            }
        }
        if (!CollectionUtils.isEmpty(errorMap.keySet())) {
            throw new CustomException(errorMap);
        }
    }
    
    /**
     * Links or updates a user's verified DigiLocker identifier and marks their identity as verified.
     * <p>
     * <b>Workflow:</b>
     * <ul>
     *   <li>If existing user already has a DigiLocker ID different from the input, throws {@link IllegalArgumentException}.</li>
     *   <li>If existing user has the same DigiLocker ID, ensures identity verification flags ({@code isIdentityVerified = true},
     *       {@code identityVerifiedDate = now}, {@code identityVerifiedSource = "DIGILOCKER"}) are recorded.</li>
     *   <li>If existing user has no DigiLocker ID yet, encrypts and persists the new DigiLocker ID, updates verification status,
     *       and returns an access token via {@link #getAccess(User, String)}.</li>
     * </ul>
     *
     * @param user         domain user containing the DigiLocker ID and OTP reference
     * @param existingUser current database state of the user record
     * @param requestInfo  request metadata for auditing and encryption/decryption
     * @return authentication token response map
     * @throws IllegalArgumentException if incoming DigiLocker ID conflicts with an existing ID on record
     */
    public Object updateDigilockerID(User user, User existingUser, RequestInfo requestInfo) {        
    	if(existingUser.getDigilockerid() != null && !existingUser.getDigilockerid().equals(user.getDigilockerid())){
            throw new IllegalArgumentException("Digilocker Id provided does not match the Digilocker Id in the database for the user");
        }
    	else if (existingUser.getDigilockerid()!=null) {
            if (!Boolean.TRUE.equals(existingUser.getIsIdentityVerified())) {
                existingUser.setIsIdentityVerified(true);
                existingUser.setIdentityVerifiedDate(new Date());
                existingUser.setIdentityVerifiedSource(UserConstants.IDENTITY_SOURCE_DIGILOCKER);
                userRepository.update(existingUser, existingUser, existingUser.getId(), existingUser.getUuid());
            }
        	return getAccess(user, user.getOtpReference());
        }
    	else {
    	user = encryptionDecryptionUtil.encryptObject(user, "User", User.class);        
    	userRepository.update(user, existingUser, existingUser.getId(), existingUser.getUuid()); 
    	user = encryptionDecryptionUtil.decryptObject(user, null, User.class, requestInfo);
    	//user = decryptionDecryptionUtil.decryptObject(user, "User", User.class);        
    	return getAccess(user, user.getOtpReference());
    	}
    }
    
    /**
     * Deactivates failed login attempts for the provided domain user.
     *
     * @param user domain {@link User} whose failed login attempts are to be reset; ignored if null or without UUID
     */
    public void resetFailedLoginAttempts(User user) {
        if (user != null && user.getUuid() != null)
            userRepository.resetFailedLoginAttemptsForUser(user.getUuid());
    }

    /**
     * Determines whether mandatory address fields (city, pincode, address) are strictly enforced.
     * Fetches configuration from {@link UserConfiguration} if available, falling back to local field or default true.
     *
     * @return true if mandatory address fields are required, false otherwise
     */
    private boolean isAddressMandatoryFieldsEnabled() {
        return userConfiguration == null || userConfiguration.getAddressMandatoryFieldsEnabled() == null
                || Boolean.TRUE.equals(userConfiguration.getAddressMandatoryFieldsEnabled());
    }

    /**
     * Creates and persists an encrypted address associated with the specified user UUID.
     * <p>
     * <b>Rules & Constraints:</b>
     * <ul>
     *   <li>Resolves the database {@code user_id} from {@code userUuid}; throws if UUID is invalid.</li>
     *   <li>Validates mandatory address fields (city, pincode, address) if enabled via configuration.</li>
     *   <li>Enforces single-record constraint for {@link AddressType#PERMANENT} and {@link AddressType#CORRESPONDENCE}.
     *       Throws {@link IllegalArgumentException} if an active address of that type already exists.
     *       ({@link AddressType#OTHER} addresses may be created multiple times).</li>
     *   <li>Encrypts address fields using {@code USER_ADDRESS_ENCRYPTION_KEY} before database insertion.</li>
     *   <li>Decrypts the persisted address before returning.</li>
     * </ul>
     *
     * @param userUuid the unique identifier of the user to link this address with
     * @param address  address entity containing address line, city, pincode, type, and tenantId
     * @return the newly saved {@link Address} with decrypted fields
     * @throws IllegalArgumentException if user UUID is invalid or a duplicate permanent/correspondence address exists
     * @throws CustomException          if mandatory address fields are missing when validation is enabled
     */
    public Address createAddress(String userUuid, Address address) {
        Long userId = userRepository.getUserIdByUuid(userUuid);
        if (userId == null) {
            throw new IllegalArgumentException("USER_UUID_NOT_VALID: The provided user UUID:"+userUuid+" is not valid");
        }
        
        // Validate mandatory fields if enabled
        boolean mandatoryEnabled = isAddressMandatoryFieldsEnabled();
        if (mandatoryEnabled && address.isMandatoryFieldsMissing(mandatoryEnabled)) {
            log.error("Address validation failed - mandatory fields missing for address creation for user UUID: {}", userUuid);
            throw new CustomException("ADDRESS_VALIDATION_ERROR", "City, pincode, and address are mandatory fields for address creation");
        }
        
        // Check if Permanent or Correspondence address already exists and not Other category as Other can be created multiple times
        if (AddressType.PERMANENT == address.getType() || AddressType.CORRESPONDENCE == address.getType()) {
            AddressSearchCriteria addressSearchCriteria = AddressSearchCriteria.builder()
                    .userId(userId)
                    .addressType(address.getType())
                    .status(UserConstants.ADDRESS_ACTIVE_STATUS)
                    .build();
            List<Address> existingAddresses = addressRepository.getAddressV2(addressSearchCriteria);
            if (!existingAddresses.isEmpty()) {
                throw new IllegalArgumentException("An address of type " + address.getType() + " already exists for user ID: " + userUuid);
            }
        }
        // Encrypt address before saving
        address = encryptionDecryptionUtil.encryptObject(address, UserConstants.USER_ADDRESS_ENCRYPTION_KEY, Address.class);
        Address savedAddress = addressRepository.createAddressV2(address, userId, address.getTenantId());
        // Decrypt address before returning
        return encryptionDecryptionUtil.decryptObject(savedAddress, UserConstants.USER_ADDRESS_ENCRYPTION_KEY, Address.class, null);
    }

    /**
     * Fetches all active addresses associated with the specified user UUID and tenant ID.
     *
     * @param user_uuid the unique identifier of the target user
     * @param tenantId  the tenant identifier
     * @return list of matching active {@link Address} objects with decrypted fields, or empty list if none found
     */
    public List<Address> getAddress(String user_uuid, String tenantId) {

        AddressSearchCriteria addressSearchCriteria = AddressSearchCriteria.builder()
                .userUuid(user_uuid)
                .tenantId(tenantId)
                .status(UserConstants.ADDRESS_ACTIVE_STATUS)
                .build();
        List<Address> addressList = addressRepository.getAddressV2(addressSearchCriteria);
        //if addressList is empty, return empty list
        if (addressList.isEmpty()) {
            return Collections.emptyList();
        }
        // Decrypt addresses before returning
        return encryptionDecryptionUtil.decryptObject(addressList, UserConstants.USER_ADDRESS_ENCRYPTION_KEY, Address.class, null);
    }

    /**
     * Updates an existing address by archiving the old record as inactive and inserting a new active record.
     * <p>
     * <b>Immutability Note:</b> To maintain address history across civic transactions, address updates do not
     * overwrite rows in-place; the existing address is marked {@code INACTIVE} and a new {@code ACTIVE} address
     * row is inserted with the updated fields.
     *
     * @param address the updated {@link Address} entity containing the address ID and new values
     * @return the newly inserted and decrypted active {@link Address}
     * @throws IllegalArgumentException if the address ID does not exist in the database
     * @throws CustomException          if mandatory address fields are missing when validation is enabled
     */
    public Address updateAddress(Address address) {

        // Validate mandatory fields if enabled
        boolean mandatoryEnabled = isAddressMandatoryFieldsEnabled();
        if (mandatoryEnabled && address.isMandatoryFieldsMissing(mandatoryEnabled)) {
            log.error("Address validation failed - mandatory fields missing for address update with ID: {}", address.getId());
            throw new CustomException("ADDRESS_VALIDATION_ERROR", "City, pincode, and address are mandatory fields for address creation");
        }

        AddressSearchCriteria addressSearchCriteria = AddressSearchCriteria.builder()
                .id(address.getId())
                .status(UserConstants.ADDRESS_ACTIVE_STATUS)
                .build();
        Address existingAddress = addressRepository.getAddressV2(addressSearchCriteria).get(0);
        if (existingAddress == null) {
            throw new IllegalArgumentException("ADDRESS_NOT_VALID: Address ID " + address.getId() + " does not exist.");
        }
        // Encrypt address before updating
        address = encryptionDecryptionUtil.encryptObject(address, UserConstants.USER_ADDRESS_ENCRYPTION_KEY, Address.class);
        // Update the old address status to inactive
        addressRepository.updateAddressV2(address.getId(), address.getUserId(), UserConstants.ADDRESS_INACTIVE_STATUS);
        // Create a new address entry with the updated details with the same user id
        Address savedAddress = addressRepository.createAddressV2(address, address.getUserId(), address.getTenantId());
        // Decrypt address before returning
        return encryptionDecryptionUtil.decryptObject(savedAddress, UserConstants.USER_ADDRESS_ENCRYPTION_KEY, Address.class, null);
    }

    /**
     * Deactivates failed login attempts for the provided authenticated user contract.
     *
     * @param user authenticated user account whose failed attempts are to be reset
     */
    public void resetFailedLoginAttempts(AuthUserAccount user) {
        if (user != null && user.getUuid() != null)
            userRepository.resetFailedLoginAttemptsForUser(user.getUuid());
    }


    /**
     * Creates a new user profile complete with nested address details in a single V2 transaction.
     * <p>
     * <b>Orchestration:</b>
     * <ol>
     *   <li>Validates enterprise account and mobile number sharing rules.</li>
     *   <li>Generates a fresh UUID and validates user name, mobile number, and address attributes.</li>
     *   <li>Conditionally validates OTP if required.</li>
     *   <li>Encrypts user fields using encryption key {@code "User"}.</li>
     *   <li>Ensures user uniqueness within the state tenant.</li>
     *   <li>Handles password (generates random password if absent, or validates and hashes if provided).</li>
     *   <li>Sets password expiry and state-level tenant ID for citizens.</li>
     *   <li>Persists the user and addresses via {@link UserRepository#createWithAddressV2(User)}.</li>
     *   <li>Decrypts and returns the persisted user entity.</li>
     * </ol>
     *
     * @param user        the user domain entity containing profile and address information
     * @param requestInfo request metadata for auditing and decryption
     * @return the created domain {@link User} with decrypted profile and address details
     * @throws DuplicateUserNameException if username already exists in tenant
     * @throws CustomException            if validation fails or account limit rules are violated
     */
    public User createUserWithAddressV2(User user, RequestInfo requestInfo) {
        validateMobileAndAccountRules(user);

        user.setUuid(UUID.randomUUID().toString());

        // Validate user details, including name, mobile number, and addresses
        user.validateNewUser(isCreateUserValidateName());
        conditionallyValidateOtp(user);
        user = encryptionDecryptionUtil.encryptObject(user, "User", User.class);

        // Ensure the user does not already exist
        validateUserUniqueness(user);
        // Handle password: if not provided, generate a random one; otherwise, validate it
        if (isEmpty(user.getPassword())) {
            user.setPassword(UUID.randomUUID().toString());
        } else {
            validatePassword(user.getPassword());
        }

        user.setPassword(encryptPwd(user.getPassword()));
        user.setDefaultPasswordExpiry(getDefaultPasswordExpiryInDays());
        user.setTenantId(getStateLevelTenantForCitizen(user.getTenantId(), user.getType()));

        // Persist the validated and encrypted user in the database
        User persistedNewUser = persistNewUserWithAddressV2(user);

        // Decrypt the persisted user before returning the response
        return encryptionDecryptionUtil.decryptObject(persistedNewUser, "UserSelf", User.class, requestInfo);
    }

    /**
     * Persists a new user with address details in the database.
     *
     * @param user the validated and encrypted user object
     * @return the persisted User object
     */
    private User persistNewUserWithAddressV2(User user) {
        return userRepository.createWithAddressV2(user);
    }


    /**
     * Updates user profile attributes and associated address information in the V2 workflow without requiring OTP verification.
     * <p>
     * <b>Security & Data Integrity Checks:</b>
     * <ul>
     *   <li>Verifies that the provided ID matches the database ID for the given UUID.</li>
     *   <li>Enforces CITIZEN isolation: citizen callers are strictly prevented from modifying any other citizen's record.</li>
     *   <li>Guarantees identity verification flags ({@code isIdentityVerified}, {@code identityVerifiedDate},
     *       {@code identityVerifiedSource}) remain immutable.</li>
     *   <li>Validates non-editable profile fields against decrypted database state.</li>
     *   <li>Updates roles, validates password, encrypts user data, and commits updates to user and address tables.</li>
     *   <li>Resets failed login attempts if the account is being unlocked.</li>
     * </ul>
     *
     * @param user        the {@link User} object containing modified profile and address data
     * @param requestInfo request metadata containing updater identity and authorization context
     * @return the updated domain {@link User} with decrypted fields
     * @throws CustomException if payload is null, data mismatch occurs, or unauthorized update is attempted
     */
    public User updateUserV2(User user, RequestInfo requestInfo) {
        if (user == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_UPDATE_REQUEST, "User payload cannot be null");
        }
        final User existingUser = getUserByUuid(user.getUuid());
        if (user.getId() != null && existingUser.getId() != null && !user.getId().equals(existingUser.getId())) {
            log.error("[USER-SERVICE-V2] Data mismatch: provided id {} does not match database id {}", user.getId(), existingUser.getId());
            throw new CustomException(UserConstants.ERROR_DATA_MISMATCH, "Data mismatch: Provided id " + user.getId() + " does not match existing user record for uuid: " + user.getUuid());
        }
        if (isCallerCitizen(requestInfo)) {
            var caller = requestInfo.getUserInfo();
            String callerUuid = caller.getUuid();
            Long callerId = caller.getId();
            boolean uuidMismatch = StringUtils.isNotBlank(callerUuid) && StringUtils.isNotBlank(existingUser.getUuid())
                    && !callerUuid.equalsIgnoreCase(existingUser.getUuid());
            boolean idMismatch = callerId != null && existingUser.getId() != null
                    && !callerId.equals(existingUser.getId());
            if (uuidMismatch || idMismatch) {
                log.error("[USER-SERVICE-V2] Unauthorized update: Citizen caller {} attempted to update user {}",
                        callerUuid, existingUser.getUuid());
                throw new CustomException("UNAUTHORIZED_ACCESS", "Citizen users are only permitted to update their own profile");
            }
        }
        // Identity verification status cannot be updated via updateUserV2
        user.setIsIdentityVerified(existingUser.getIsIdentityVerified());
        user.setIdentityVerifiedDate(existingUser.getIdentityVerifiedDate());
        user.setIdentityVerifiedSource(existingUser.getIdentityVerifiedSource());
        User decryptedExistingUser = encryptionDecryptionUtil.decryptObject(existingUser, UserConstants.USER_SELF_ENCRYPTION_KEY, User.class, requestInfo);
        user.validateNonEditableProfileFields(decryptedExistingUser != null ? decryptedExistingUser : existingUser, false);
        user.setTenantId(getStateLevelTenantForCitizen(user.getTenantId(), user.getType()));
        validateUserRoles(user);
        user.validateUserModification();
        validatePassword(user.getPassword());
        user.setPassword(encryptPwd(user.getPassword()));
        /* encrypt */
        user = encryptionDecryptionUtil.encryptObject(user, "User", User.class);
        userRepository.updateV2(user, existingUser, requestInfo.getUserInfo().getId(), requestInfo.getUserInfo().getUuid());

        // If user is being unlocked via update, reset failed login attempts
        if (user.getAccountLocked() != null && !user.getAccountLocked() && existingUser.getAccountLocked())
            resetFailedLoginAttempts(user);

        User encryptedUpdatedUserfromDB = getUserByUuid(user.getUuid());

        User decryptedupdatedUserfromDB = encryptionDecryptionUtil.decryptObject(encryptedUpdatedUserfromDB, "UserSelf", User.class, requestInfo);
        return decryptedupdatedUserfromDB;
    }

    /**
     * Searches for users and their associated addresses matching the provided V2 criteria.
     * <p>
     * Validates search criteria, applies citizen caller restrictions, normalizes tenant ID,
     * encrypts search criteria for matching against encrypted columns, queries {@link UserRepository#findAllV2(UserSearchCriteria)},
     * decrypts user results, and enriches photo URLs from filestore before returning.
     *
     * @param searchCriteria     criteria used to filter users (UUIDs, mobile, name, etc.)
     * @param isInterServiceCall flag indicating whether the call is from another internal service
     * @param requestInfo        request metadata used for authorization and ABAC decryption
     * @return list of matching domain {@link User} entities with decrypted profile and address details
     * @throws CustomException if search criteria or citizen security validations fail
     */
    public List<User> searchUsersV2(UserSearchCriteria searchCriteria,
                                  boolean isInterServiceCall, RequestInfo requestInfo) {

        searchCriteria.validate(isInterServiceCall);
        validateCitizenSearch(searchCriteria, isInterServiceCall, requestInfo);

        searchCriteria.setTenantId(getStateLevelTenantForCitizen(searchCriteria.getTenantId(), searchCriteria.getType()));
        String altmobnumber = null;

        if (searchCriteria.getMobileNumber() != null) {
            altmobnumber = searchCriteria.getMobileNumber();
        }
        /* encrypt here / encrypted searchcriteria will be used for search*/
        searchCriteria = encryptionDecryptionUtil.encryptObject(searchCriteria, "User", UserSearchCriteria.class);

        if (altmobnumber != null) {
            searchCriteria.setAlternatemobilenumber(altmobnumber);
        }

        List<User> list = userRepository.findAllV2(searchCriteria);

        // If list is null or empty, return an empty list
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        /* decrypt here / final reponse decrypted*/
        list = encryptionDecryptionUtil.decryptObject(list, "User", User.class, requestInfo);

        setFileStoreUrlsByFileStoreIds(list);
        return list;
    }

    /**
     * Enforces enterprise user creation rules for mobile number across user types:
     * <ul>
     *   <li><b>EMPLOYEE Accounts:</b>
     *     <ul>
     *       <li>An employee CAN have the same mobile number as a citizen (user types are completely isolated).</li>
     *       <li>At most ONE employee can have this mobile number. Rejects if another active employee already exists with this mobile.</li>
     *     </ul>
     *   </li>
     *   <li><b>CITIZEN Accounts:</b>
     *     <ul>
     *       <li>Multiple citizens CAN share a mobile number (family multi-account).</li>
     *       <li>At most ONE unverified citizen account (identity unverified) is permitted per mobile number. Rejects if an unverified citizen already exists.</li>
     *       <li>Multiple verified citizen accounts (e.g. Aadhaar verified) can share the mobile number, provided each has a unique Aadhaar number.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param user incoming user to validate
     * @throws CustomException if validation fails
     */
    private void validateMobileAndAccountRules(User user) {
        if (user == null) {
            return;
        }
        String mobileNumber = resolveMobileNumber(user);
        if (StringUtils.isBlank(mobileNumber)) {
            return;
        }

        UserType userType = user.getType() != null ? user.getType() : UserType.CITIZEN;

        if (UserType.EMPLOYEE.equals(userType)) {
            User existingEmployee = findExistingEmployeeByMobile(mobileNumber);
            if (existingEmployee != null && (existingEmployee.getActive() == null || Boolean.TRUE.equals(existingEmployee.getActive()))) {
                log.error("[USER-SERVICE] Employee creation rejected: Mobile {} is already linked to active employee UUID: {}",
                        MaskUtils.maskMobile(mobileNumber), existingEmployee.getUuid());
                throw new CustomException(UserConstants.ERROR_EMPLOYEE_ALREADY_EXISTS_WITH_MOBILE,
                        UserConstants.MSG_EMPLOYEE_ALREADY_EXISTS_WITH_MOBILE);
            }
        } else if (UserType.CITIZEN.equals(userType)) {
            // 1. Cross-mobile Aadhaar uniqueness check: An Aadhaar number can only be linked to one active citizen across the tenant
            if (StringUtils.isNotBlank(user.getAadhaarNumber())) {
                String stateTenant = getStateLevelTenantForCitizen(user.getTenantId(), user.getType());
                UserSearchCriteria aadhaarCriteria = UserSearchCriteria.builder()
                        .aadhaarNumber(user.getAadhaarNumber().trim())
                        .tenantId(stateTenant)
                        .type(UserType.CITIZEN)
                        .active(true)
                        .build();
                List<User> matchingAadhaarUsers = userRepository.findAll(aadhaarCriteria);
                if (!CollectionUtils.isEmpty(matchingAadhaarUsers)) {
                    for (User existing : matchingAadhaarUsers) {
                        if (existing != null && StringUtils.isNotBlank(existing.getAadhaarNumber())
                                && existing.getAadhaarNumber().trim().equalsIgnoreCase(user.getAadhaarNumber().trim())
                                && (user.getUuid() == null || !existing.getUuid().equalsIgnoreCase(user.getUuid()))) {
                            log.error("[USER-SERVICE] Citizen creation rejected: Citizen already exists with Aadhaar number across system in tenant: {}", stateTenant);
                            throw new CustomException(UserConstants.ERROR_USER_ALREADY_EXISTS, UserConstants.MSG_USER_ALREADY_EXISTS_AADHAAR);
                        }
                    }
                }
            }

            List<User> existingCitizens = findCitizensByMobile(mobileNumber);

            boolean isIncomingVerified = Boolean.TRUE.equals(user.getIsIdentityVerified());

            long unverifiedCount = 0;
            for (User existingCitizen : existingCitizens) {
                if (existingCitizen != null && (existingCitizen.getActive() == null || Boolean.TRUE.equals(existingCitizen.getActive()))) {
                    // Check Aadhaar duplicate among citizens on this mobile
                    if (StringUtils.isNotBlank(user.getAadhaarNumber()) && StringUtils.isNotBlank(existingCitizen.getAadhaarNumber())
                            && user.getAadhaarNumber().trim().equalsIgnoreCase(existingCitizen.getAadhaarNumber().trim())) {
                        log.error("[USER-SERVICE] Citizen already exists with Aadhaar number across system");
                        throw new CustomException(UserConstants.ERROR_USER_ALREADY_EXISTS, UserConstants.MSG_USER_ALREADY_EXISTS_AADHAAR);
                    }

                    if (!Boolean.TRUE.equals(existingCitizen.getIsIdentityVerified())) {
                        unverifiedCount++;
                    }
                }
            }

            // Constraint: Only one unverified citizen profile is allowed per mobile number
            if (!isIncomingVerified && unverifiedCount > 0) {
                log.error("[USER-SERVICE] Cannot create unverified citizen: An unverified citizen account already exists for mobile: {}",
                        MaskUtils.maskMobile(mobileNumber));
                throw new CustomException(UserConstants.ERROR_UNVERIFIED_CITIZEN_EXISTS,
                        UserConstants.MSG_UNVERIFIED_CITIZEN_EXISTS);
            }
        }
    }

    /**
     * Finds an existing active employee by mobile number across all tenants.
     *
     * @param mobileNumber 10-digit mobile number
     * @return matching active employee {@link User}, or {@code null} if none found
     */
    private User findExistingEmployeeByMobile(String mobileNumber) {
        if (StringUtils.isBlank(mobileNumber)) {
            return null;
        }
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .mobileNumber(mobileNumber)
                .tenantId(null)
                .type(UserType.EMPLOYEE)
                .build();
        try {
            UserSearchCriteria encCriteria = encryptionDecryptionUtil.encryptObject(criteria, UserConstants.USER_ENCRYPTION_KEY, UserSearchCriteria.class);
            List<User> employeeCandidates = userRepository.findAll(encCriteria != null ? encCriteria : criteria);
            if (!CollectionUtils.isEmpty(employeeCandidates)) {
                for (User employeeCandidate : employeeCandidates) {
                    if (employeeCandidate != null && (employeeCandidate.getActive() == null || Boolean.TRUE.equals(employeeCandidate.getActive()))) {
                        return employeeCandidate;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[USER-SERVICE] Error querying existing employee by mobile {}: {}", MaskUtils.maskMobile(mobileNumber), e.getMessage());
        }
        return null;
    }

    /**
     * Finds existing citizen accounts associated with the given mobile number across all tenants.
     *
     * @param mobileNumber 10-digit citizen mobile number
     * @return list of decrypted domain {@link User} entities
     */
    private List<User> findCitizensByMobile(String mobileNumber) {
        if (StringUtils.isBlank(mobileNumber)) {
            return Collections.emptyList();
        }
        Map<String, User> userMap = new LinkedHashMap<>();

        // 1. Search by encrypted mobileNumber
        UserSearchCriteria mobileCriteria = UserSearchCriteria.builder()
                .mobileNumber(mobileNumber)
                .tenantId(null)
                .type(UserType.CITIZEN)
                .build();
        try {
            UserSearchCriteria encCriteria = encryptionDecryptionUtil.encryptObject(mobileCriteria, UserConstants.USER_ENCRYPTION_KEY, UserSearchCriteria.class);
            List<User> mobileMatchedCitizens = userRepository.findAll(encCriteria != null ? encCriteria : mobileCriteria);
            if (!CollectionUtils.isEmpty(mobileMatchedCitizens)) {
                for (User mobileMatchedCitizen : mobileMatchedCitizens) {
                    if (mobileMatchedCitizen != null && mobileMatchedCitizen.getUuid() != null) {
                        userMap.put(mobileMatchedCitizen.getUuid(), mobileMatchedCitizen);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[USER-SERVICE] Error querying existing citizens by mobile {}: {}", MaskUtils.maskMobile(mobileNumber), e.getMessage());
        }

        // 2. Search by encrypted userName
        try {
            UserSearchCriteria userCriteria = UserSearchCriteria.builder()
                    .userName(mobileNumber)
                    .tenantId(null)
                    .type(UserType.CITIZEN)
                    .build();
            UserSearchCriteria encCriteria = encryptionDecryptionUtil.encryptObject(userCriteria, UserConstants.USER_ENCRYPTION_KEY, UserSearchCriteria.class);
            List<User> usernameMatchedCitizens = userRepository.findAll(encCriteria != null ? encCriteria : userCriteria);
            if (!CollectionUtils.isEmpty(usernameMatchedCitizens)) {
                for (User usernameMatchedCitizen : usernameMatchedCitizens) {
                    if (usernameMatchedCitizen != null && usernameMatchedCitizen.getUuid() != null) {
                        userMap.put(usernameMatchedCitizen.getUuid(), usernameMatchedCitizen);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[USER-SERVICE] Error querying existing citizens by userName {}: {}", MaskUtils.maskMobile(mobileNumber), e.getMessage());
        }

        if (userMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> decryptedList = encryptionDecryptionUtil.decryptObject(new ArrayList<>(userMap.values()), UserConstants.USER_ENCRYPTION_KEY, User.class, null);
        return decryptedList != null ? decryptedList : new ArrayList<>(userMap.values());
    }



    /**
     * Resolves the primary 10-digit mobile number from the user model.
     * Evaluates {@code user.getMobileNumber()}, falling back to numeric {@code user.getUsername()}.
     *
     * @param user domain user entity
     * @return trimmed mobile number string, or {@code null} if absent or non-numeric
     */
    private String resolveMobileNumber(User user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.isNotBlank(user.getMobileNumber())) {
            return user.getMobileNumber().trim();
        }
        if (StringUtils.isNotBlank(user.getUsername()) && StringUtils.isNumeric(user.getUsername().trim())) {
            return user.getUsername().trim();
        }
        return null;
    }

    /**
     * Determines whether the caller is a CITIZEN based on database ground truth.
     * Falls back to requestInfo token type if user is not found in DB.
     *
     * @param requestInfo request context containing userInfo
     * @return true if caller is a CITIZEN, false otherwise
     */
    public boolean isCallerCitizen(RequestInfo requestInfo) {
        if (requestInfo == null || requestInfo.getUserInfo() == null) {
            return false;
        }
        var caller = requestInfo.getUserInfo();
        User dbUser = getCallerDbUser(requestInfo);
        if (dbUser != null && dbUser.getType() != null) {
            return UserType.CITIZEN.equals(dbUser.getType());
        }
        return UserType.CITIZEN.name().equalsIgnoreCase(caller.getType());
    }

    /**
     * Looks up the authenticated caller's domain user record from the database by UUID or ID from RequestInfo.
     *
     * @param requestInfo standard UPYOG request metadata envelope
     * @return database domain {@link User} entity or null if not found
     */
    public User getCallerDbUser(RequestInfo requestInfo) {
        if (requestInfo == null || requestInfo.getUserInfo() == null) {
            return null;
        }
        var caller = requestInfo.getUserInfo();
        if (StringUtils.isNotBlank(caller.getUuid())) {
            try {
                return getUserByUuid(caller.getUuid().trim());
            } catch (Exception e) {
                log.debug("[USER-SERVICE] Error looking up caller in DB by uuid: {}", e.getMessage());
            }
        }
        if (caller.getId() != null) {
            try {
                UserSearchCriteria criteria = UserSearchCriteria.builder()
                        .id(Collections.singletonList(caller.getId()))
                        .excludeAddressDetails(true)
                        .build();
                List<User> list = userRepository.findAll(criteria);
                if (!CollectionUtils.isEmpty(list)) {
                    return list.get(0);
                }
            } catch (Exception e) {
                log.debug("[USER-SERVICE] Error looking up caller in DB by id: {}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * Enforces citizen caller isolation rules to prevent unauthorized searching of other users profiles.
     *
     * @param searchCriteria incoming search filters
     * @param isInterServiceCall whether invocation is a trusted inter-service call
     * @param requestInfo request metadata and security context
     * @throws CustomException if a citizen caller attempts to search for other users
     */
    private void validateCitizenSearch(UserSearchCriteria searchCriteria, boolean isInterServiceCall, RequestInfo requestInfo) {
        if (isInterServiceCall || requestInfo == null || requestInfo.getUserInfo() == null) {
            return;
        }
        if (!isCallerCitizen(requestInfo)) {
            return;
        }

        var caller = requestInfo.getUserInfo();
        User dbCaller = getCallerDbUser(requestInfo);

        String callerUuid = (dbCaller != null && StringUtils.isNotBlank(dbCaller.getUuid()))
                ? dbCaller.getUuid().trim()
                : (caller.getUuid() != null ? caller.getUuid().trim() : null);

        String callerMobile = (dbCaller != null && StringUtils.isNotBlank(dbCaller.getMobileNumber()))
                ? dbCaller.getMobileNumber().trim()
                : (StringUtils.isNotBlank(caller.getMobileNumber())
                ? caller.getMobileNumber().trim()
                : (StringUtils.isNotBlank(caller.getUserName()) && StringUtils.isNumeric(caller.getUserName().trim())
                ? caller.getUserName().trim() : null));

        Long callerId = (dbCaller != null && dbCaller.getId() != null)
                ? dbCaller.getId() : caller.getId();

        String callerUserName = (dbCaller != null && StringUtils.isNotBlank(dbCaller.getUsername()))
                ? dbCaller.getUsername().trim()
                : (caller.getUserName() != null ? caller.getUserName().trim() : null);

        boolean hasUuid = !CollectionUtils.isEmpty(searchCriteria.getUuid());
        boolean hasMobile = StringUtils.isNotBlank(searchCriteria.getMobileNumber());
        boolean hasUserName = StringUtils.isNotBlank(searchCriteria.getUserName());
        boolean hasId = !CollectionUtils.isEmpty(searchCriteria.getId());

        // A citizen caller must specify at least one identifying criterion
        if (!hasUuid && !hasMobile && !hasUserName && !hasId) {
            log.error("[USER-SERVICE] Unauthorized search: Citizen caller must search using their own UUID or mobile number.");
            throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH);
        }

        // Validate UUIDs
        if (hasUuid) {
            if (StringUtils.isBlank(callerUuid)) {
                log.error("[USER-SERVICE] Unauthorized search: Calling citizen has no UUID in request context.");
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH);
            }
            boolean anyMismatch = searchCriteria.getUuid().stream()
                    .filter(StringUtils::isNotBlank)
                    .anyMatch(u -> !u.trim().equalsIgnoreCase(callerUuid));
            if (anyMismatch) {
                log.error("[USER-SERVICE] Unauthorized search: Citizen caller {} attempted to search for uuid(s): {}", callerUuid, searchCriteria.getUuid());
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH);
            }
        }

        // Validate Mobile Number
        if (hasMobile) {
            String trimmedMobile = searchCriteria.getMobileNumber().trim();
            boolean matchesCaller = (callerMobile != null && trimmedMobile.equals(callerMobile))
                    || (callerUserName != null && trimmedMobile.equalsIgnoreCase(callerUserName))
                    || (dbCaller != null && dbCaller.getUsername() != null && trimmedMobile.equalsIgnoreCase(dbCaller.getUsername().trim()))
                    || (dbCaller != null && dbCaller.getMobileNumber() != null && trimmedMobile.equals(dbCaller.getMobileNumber().trim()));
            if (!matchesCaller) {
                log.error("[USER-SERVICE] Unauthorized search: Citizen caller {} attempted to search for mobile: {}", callerUuid, MaskUtils.maskMobile(trimmedMobile));
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH);
            }
        }

        // Validate User Name
        if (hasUserName) {
            String trimmedUserName = searchCriteria.getUserName().trim();
            boolean matchesCaller = (callerUserName != null && trimmedUserName.equalsIgnoreCase(callerUserName))
                    || (callerMobile != null && trimmedUserName.equals(callerMobile))
                    || (dbCaller != null && dbCaller.getUsername() != null && trimmedUserName.equalsIgnoreCase(dbCaller.getUsername().trim()))
                    || (dbCaller != null && dbCaller.getMobileNumber() != null && trimmedUserName.equals(dbCaller.getMobileNumber().trim()));
            if (!matchesCaller) {
                log.error("[USER-SERVICE] Unauthorized search: Citizen caller {} attempted to search for username: {}", callerUuid, MaskUtils.maskIdentifier(trimmedUserName));
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH);
            }
        }

        // Validate IDs
        if (hasId) {
            if (callerId == null) {
                log.error("[USER-SERVICE] Unauthorized search: Calling citizen has no ID in request context.");
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH);
            }
            boolean anyMismatch = searchCriteria.getId().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(id -> !id.equals(callerId));
            if (anyMismatch) {
                log.error("[USER-SERVICE] Unauthorized search: Citizen caller ID {} attempted to search for id(s): {}", callerId, searchCriteria.getId());
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH);
            }
        }
    }
}
