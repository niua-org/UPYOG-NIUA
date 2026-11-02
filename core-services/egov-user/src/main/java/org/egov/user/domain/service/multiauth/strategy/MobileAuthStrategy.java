package org.egov.user.domain.service.multiauth.strategy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.UserService;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.OtpRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.Otp;
import org.egov.user.web.contract.OtpValidateRequest;
import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.multiauth.LoginV2Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.egov.user.config.UserConfiguration;

/**
 * Authentication strategy for standard mobile number and OTP verification.
 *
 * <p>Validates OTP against egov-otp service (or configured fixed OTP for testing),
 * resolves existing citizen accounts (supporting multiple profiles sharing a mobile number),
 * or auto-creates a lightweight citizen profile.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class MobileAuthStrategy implements MultiAuthStrategy {

    private final UserService userService;
    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final EncryptionDecryptionUtil encryptionDecryptionUtil;

    private final UserConfiguration userConfiguration;
    private final Map<String, Integer> invalidOtpAttempts = new ConcurrentHashMap<>();

    /**
     * Primary constructor injecting repositories, encryption utility, and configuration.
     *
     * @param userService              user domain service for provisioning
     * @param userRepository           user repository for persistence lookups
     * @param otpRepository            OTP repository for OTP validation
     * @param encryptionDecryptionUtil utility for encrypted criteria and decryption
     * @param userConfiguration        application configuration properties
     */
    @Autowired
    public MobileAuthStrategy(UserService userService,
                              UserRepository userRepository,
                              OtpRepository otpRepository,
                              EncryptionDecryptionUtil encryptionDecryptionUtil,
                              @Autowired(required = false) UserConfiguration userConfiguration) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.encryptionDecryptionUtil = encryptionDecryptionUtil;
        this.userConfiguration = userConfiguration;
    }

    /**
     * Constructor for unit tests without configuration properties.
     *
     * @param userService              user domain service for provisioning
     * @param userRepository           user repository for persistence lookups
     * @param otpRepository            OTP repository for OTP validation
     * @param encryptionDecryptionUtil utility for encrypted criteria and decryption
     */
    public MobileAuthStrategy(UserService userService,
                              UserRepository userRepository,
                              OtpRepository otpRepository,
                              EncryptionDecryptionUtil encryptionDecryptionUtil) {
        this(userService, userRepository, otpRepository, encryptionDecryptionUtil, null);
    }

    private boolean isFixedOTPEnabled() {
        return userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getCitizenLoginPasswordOtpFixedEnabled());
    }

    private boolean isMultiAccountSelectionEnabled() {
        return userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getCitizenMultiAccountSelectionEnabled());
    }

    private User selectPrimaryCitizenUser(List<User> matchedUsers, String mobileNumber) {
        if (CollectionUtils.isEmpty(matchedUsers)) {
            return null;
        }
        log.info("[MOBILE-AUTH-STRATEGY] Selecting primary citizen user for mobile: {} from {} candidates",
                maskMobile(mobileNumber), matchedUsers.size());

        // 1. Prioritize active user whose username matches the login mobile number
        for (User u : matchedUsers) {
            if (isUsernameMatchingMobile(u.getUsername(), mobileNumber) && !Boolean.FALSE.equals(u.getActive())) {
                log.info("[MOBILE-AUTH-STRATEGY] Selected primary citizen user with matching username: {} (UUID: {})",
                        u.getUsername(), u.getUuid());
                return u;
            }
        }

        // 2. If user whose username matches mobile number exists but is inactive, enforce inactive error
        for (User u : matchedUsers) {
            if (isUsernameMatchingMobile(u.getUsername(), mobileNumber) && Boolean.FALSE.equals(u.getActive())) {
                log.error("[MOBILE-AUTH-STRATEGY] Primary citizen profile (username={}) for mobile {} is inactive",
                        u.getUsername(), maskMobile(mobileNumber));
                throw new CustomException(UserConstants.ERROR_ACCOUNT_INACTIVE, UserConstants.MSG_ACCOUNT_INACTIVE);
            }
        }

        // 3. Fallback: if no user has username == mobileNumber, pick the first active user
        for (User u : matchedUsers) {
            if (!Boolean.FALSE.equals(u.getActive())) {
                log.warn("[MOBILE-AUTH-STRATEGY] No candidate has username matching mobile {}. Falling back to active candidate: {} (UUID: {})",
                        maskMobile(mobileNumber), u.getUsername(), u.getUuid());
                return u;
            }
        }

        log.error("[MOBILE-AUTH-STRATEGY] All matching citizen accounts for mobile {} are inactive", maskMobile(mobileNumber));
        throw new CustomException(UserConstants.ERROR_ACCOUNT_INACTIVE, UserConstants.MSG_ACCOUNT_INACTIVE);
    }

    private boolean isUsernameMatchingMobile(String username, String mobileNumber) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(mobileNumber)) {
            return false;
        }
        String uTrim = username.trim();
        String mTrim = mobileNumber.trim();
        if (uTrim.equalsIgnoreCase(mTrim)) {
            return true;
        }
        String stdMobile = extractStandard10DigitMobile(mTrim);
        String stdUser = extractStandard10DigitMobile(uTrim);
        return stdMobile != null && stdUser != null && stdMobile.equals(stdUser) && uTrim.matches("^[0-9+ ]+$");
    }

    private String extractStandard10DigitMobile(String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        String digits = input.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return digits;
        }
        if (digits.length() > 10 && (digits.startsWith("91") || digits.startsWith("0"))) {
            return digits.substring(digits.length() - 10);
        }
        return digits;
    }

    private String getFixedOTPPassword() {
        return (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getCitizenLoginPasswordOtpFixedValue()))
                ? userConfiguration.getCitizenLoginPasswordOtpFixedValue()
                : "123456";
    }

    private boolean isRoleStateLevel() {
        return userConfiguration == null || Boolean.TRUE.equals(userConfiguration.getRolesStateLevelEnabled());
    }

    private int getMaxOtpAttempts() {
        return (userConfiguration != null && userConfiguration.getOtpMaxInvalidAttempts() != null)
                ? userConfiguration.getOtpMaxInvalidAttempts()
                : 3;
    }

    /**
     * Checks if this strategy supports the given authentication channel type.
     *
     * @param authType the authentication channel type
     * @return true if authType is MOBILE_NUMBER, false otherwise
     */
    @Override
    public boolean supports(AuthType authType) {
        return AuthType.MOBILE_NUMBER.equals(authType);
    }

    /**
     * Determines whether this strategy can handle the incoming login request based on presence of mobileNumber alone.
     *
     * @param request the login request payload
     * @return true if request has mobileNumber and no third-party identifiers (Aadhaar/Family)
     */
    @Override
    public boolean canHandle(LoginV2Request request) {
        return request != null
                && StringUtils.isNotBlank(request.getMobileNumber())
                && StringUtils.isBlank(request.getFamilyId())
                && StringUtils.isBlank(request.getAadhaarNumber())
                && StringUtils.isBlank(request.getTxnId())
                && StringUtils.isBlank(request.getReferenceId());
    }

    /**
     * Authenticates the citizen via mobile OTP, handles multi-account selection,
     * auto-provisions new profiles, and strictly blocks inactive user accounts.
     *
     * @param request the login request payload
     * @return result containing authenticated domain user or candidate list
     * @throws CustomException if validation fails or account is inactive
     */
    @Override
    public AuthenticatedUserResult authenticateAndResolveUser(LoginV2Request request) {
        String mobileNumber = request.getMobileNumber();
        String otp = request.getOtp();
        String tenantId = request.getTenantId();

        if (StringUtils.isBlank(mobileNumber)) {
            log.error("[MOBILE-AUTH-STRATEGY] Validation failed: mobileNumber is blank");
            throw new CustomException(UserConstants.ERROR_INVALID_MOBILE_NUMBER, UserConstants.MSG_MOBILE_AUTH_MOBILE_REQUIRED);
        }
        if (StringUtils.isBlank(otp)) {
            log.error("[MOBILE-AUTH-STRATEGY] Validation failed: OTP is blank for mobile: {}", maskMobile(mobileNumber));
            throw new CustomException(UserConstants.ERROR_INVALID_OTP, UserConstants.MSG_LOGIN_OTP_REQUIRED);
        }

        log.info("[MOBILE-AUTH-STRATEGY] Processing login for mobile: {}, tenant: {}", maskMobile(mobileNumber), tenantId);

        // 1. Validate OTP
        validateOtp(mobileNumber, otp, tenantId, request);

        // 2. Resolve existing user or auto-create
        String stateTenant = resolveStateTenant(tenantId);

        // 2a. Query all citizen accounts associated with this verified mobile number
        log.debug("[MOBILE-AUTH-STRATEGY] Querying citizen profiles for mobile: {} in tenant: {}", maskMobile(mobileNumber), stateTenant);
        List<User> matchedUsers = findUsersByMobile(mobileNumber, stateTenant, request.getRequestInfo());

        // 2b. If client specified a selected user UUID, verify that it belongs to the matched accounts for this mobile
        if (StringUtils.isNotBlank(request.getSelectedUserUuid())) {
            log.info("[MOBILE-AUTH-STRATEGY] Resolving selected user UUID: {} for mobile: {}",
                    request.getSelectedUserUuid(), maskMobile(mobileNumber));
            User selectedUser = matchedUsers.stream()
                    .filter(u -> request.getSelectedUserUuid().equals(u.getUuid()))
                    .findFirst()
                    .orElse(null);

            if (selectedUser == null) {
                log.error("[MOBILE-AUTH-STRATEGY] Selected citizen profile not found or does not belong to verified mobile: {}", request.getSelectedUserUuid());
                throw new CustomException(UserConstants.ERROR_USER_NOT_FOUND, UserConstants.MSG_SELECTED_USER_NOT_FOUND);
            }
            if (Boolean.FALSE.equals(selectedUser.getActive())) {
                log.error("[MOBILE-AUTH-STRATEGY] Selected citizen profile {} is inactive", selectedUser.getUuid());
                throw new CustomException(UserConstants.ERROR_ACCOUNT_INACTIVE, UserConstants.MSG_ACCOUNT_INACTIVE);
            }
            selectedUser.setLastLoginMethod(AuthType.MOBILE_NUMBER.name());
            return AuthenticatedUserResult.builder()
                    .user(selectedUser)
                    .isNewUser(false)
                    .authType(AuthType.MOBILE_NUMBER)
                    .build();
        }

        // 2c. Edge Case: If 2 or more users are linked to same mobile number
        if (!CollectionUtils.isEmpty(matchedUsers) && matchedUsers.size() > 1) {
            boolean allInactive = matchedUsers.stream().allMatch(u -> !Boolean.TRUE.equals(u.getActive()));
            if (allInactive) {
                log.error("[MOBILE-AUTH-STRATEGY] All matching citizen accounts for mobile {} are inactive", maskMobile(mobileNumber));
                throw new CustomException(UserConstants.ERROR_ACCOUNT_INACTIVE, UserConstants.MSG_ACCOUNT_INACTIVE);
            }
            if (isMultiAccountSelectionEnabled()) {
                log.info("[MOBILE-AUTH-STRATEGY] Found {} citizen profiles linked to mobile: {}. Returning list for selection.",
                        matchedUsers.size(), maskMobile(mobileNumber));
                return AuthenticatedUserResult.builder()
                        .multipleUsersFound(true)
                        .matchedUsers(matchedUsers)
                        .authType(AuthType.MOBILE_NUMBER)
                        .build();
            } else {
                log.info("[MOBILE-AUTH-STRATEGY] Found {} citizen profiles linked to mobile: {}. Multi-account selection disabled, returning single user.",
                        matchedUsers.size(), maskMobile(mobileNumber));
                User user = selectPrimaryCitizenUser(matchedUsers, mobileNumber);
                user.setLastLoginMethod(AuthType.MOBILE_NUMBER.name());
                return AuthenticatedUserResult.builder()
                        .user(user)
                        .isNewUser(false)
                        .authType(AuthType.MOBILE_NUMBER)
                        .build();
            }
        }

        // 2d. If exactly 1 user found, proceed with login
        if (!CollectionUtils.isEmpty(matchedUsers) && matchedUsers.size() == 1) {
            User user = matchedUsers.get(0);
            if (!Boolean.TRUE.equals(user.getActive())) {
                log.error("[MOBILE-AUTH-STRATEGY] Citizen profile ID {} for mobile {} is inactive", user.getId(), maskMobile(mobileNumber));
                throw new CustomException(UserConstants.ERROR_ACCOUNT_INACTIVE, UserConstants.MSG_ACCOUNT_INACTIVE);
            }
            log.info("[MOBILE-AUTH-STRATEGY] Matched single citizen ID: {}, UUID: {} for mobile: {}",
                    user.getId(), user.getUuid(), maskMobile(mobileNumber));
            user.setLastLoginMethod(AuthType.MOBILE_NUMBER.name());
            return AuthenticatedUserResult.builder()
                    .user(user)
                    .isNewUser(false)
                    .authType(AuthType.MOBILE_NUMBER)
                    .build();
        }

        // 2e. Citizen account not found -> Auto-provision lightweight citizen profile
        log.info("[MOBILE-AUTH-STRATEGY] Citizen account not found for mobile: {}. Auto-provisioning lightweight profile.",
                maskMobile(mobileNumber));
        String name = StringUtils.isNotBlank(request.getName()) ? request.getName().trim() : "Citizen";

        String uniqueUsername = !userRepository.isUserPresent(mobileNumber, stateTenant, UserType.CITIZEN)
                ? mobileNumber
                : mobileNumber + "_" + UUID.randomUUID().toString().substring(0, 8);

        User newUser = User.builder()
                .username(uniqueUsername)
                .name(name)
                .mobileNumber(mobileNumber)
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .active(true)
                .lastLoginMethod(AuthType.MOBILE_NUMBER.name())
                .roles(Collections.singleton(Role.getCitizenRole()))
                .otpValidationMandatory(false)
                .build();

        User createdUser = userService.createUser(newUser, request.getRequestInfo());
        log.info("[MOBILE-AUTH-STRATEGY] Successfully provisioned new citizen ID: {}, UUID: {}",
                createdUser.getId(), createdUser.getUuid());

        if (!Boolean.TRUE.equals(createdUser.getActive())) {
            log.error("[MOBILE-AUTH-STRATEGY] Existing citizen account ID {} is inactive", createdUser.getId());
            throw new CustomException(UserConstants.ERROR_ACCOUNT_INACTIVE, UserConstants.MSG_ACCOUNT_INACTIVE);
        }

        return AuthenticatedUserResult.builder()
                .user(createdUser)
                .isNewUser(true)
                .authType(AuthType.MOBILE_NUMBER)
                .build();
    }

    /**
     * Validates the supplied OTP against egov-otp service, or matches against configured fixed test OTP.
     *
     * @param mobileNumber the 10-digit mobile number receiving OTP
     * @param otp          the OTP string entered by citizen
     * @param tenantId     the tenant identifier
     * @param request      the incoming authentication request containing request context
     * @throws CustomException if OTP verification is rejected or expired
     */
    private void validateOtp(String mobileNumber, String otp, String tenantId, LoginV2Request request) {
        log.debug("[MOBILE-AUTH-STRATEGY] Validating OTP (length: {}) for mobile: {}", otp.length(), maskMobile(mobileNumber));

        if (StringUtils.isNotBlank(mobileNumber)) {
            int currentAttempts = invalidOtpAttempts.getOrDefault(mobileNumber, 0);
            if (currentAttempts >= getMaxOtpAttempts()) {
                log.error("[MOBILE-AUTH-STRATEGY] Locked: mobile {} has exceeded maximum allowed OTP attempts ({})",
                        maskMobile(mobileNumber), getMaxOtpAttempts());
                throw new CustomException(UserConstants.ERROR_MAX_OTP_ATTEMPTS_EXCEEDED, UserConstants.MSG_MAX_OTP_ATTEMPTS_EXCEEDED);
            }
        }

        if (isFixedOTPEnabled() && StringUtils.isNotBlank(getFixedOTPPassword()) && getFixedOTPPassword().equals(otp)) {
            log.info("[MOBILE-AUTH-STRATEGY] Fixed OTP matched successfully for mobile: {}", maskMobile(mobileNumber));
            if (StringUtils.isNotBlank(mobileNumber)) {
                invalidOtpAttempts.remove(mobileNumber);
            }
            return;
        }

        Otp otpContract = Otp.builder()
                .otp(otp)
                .identity(mobileNumber)
                .tenantId(tenantId)
                .build();

        OtpValidateRequest otpValidateRequest = OtpValidateRequest.builder()
                .requestInfo(request.getRequestInfo())
                .otp(otpContract)
                .build();

        boolean isOtpValid = otpRepository.validateOtp(otpValidateRequest);
        if (!isOtpValid) {
            if (StringUtils.isNotBlank(mobileNumber)) {
                int attempts = invalidOtpAttempts.compute(mobileNumber, (k, v) -> (v == null ? 1 : v + 1));
                if (attempts >= getMaxOtpAttempts()) {
                    log.error("[MOBILE-AUTH-STRATEGY] Mobile {} reached max invalid OTP attempts ({}). Invalidation triggered.",
                            maskMobile(mobileNumber), getMaxOtpAttempts());
                    throw new CustomException(UserConstants.ERROR_MAX_OTP_ATTEMPTS_EXCEEDED, UserConstants.MSG_MAX_OTP_ATTEMPTS_EXCEEDED);
                }
            }
            log.error("[MOBILE-AUTH-STRATEGY] OTP verification rejected by egov-otp for mobile: {}", maskMobile(mobileNumber));
            throw new CustomException(UserConstants.ERROR_INVALID_OTP, "Invalid or expired OTP entered for mobile: " + maskMobile(mobileNumber));
        }

        if (StringUtils.isNotBlank(mobileNumber)) {
            invalidOtpAttempts.remove(mobileNumber);
        }
        log.info("[MOBILE-AUTH-STRATEGY] OTP verified successfully via egov-otp for mobile: {}", maskMobile(mobileNumber));
    }

    /**
     * Resolves all citizen users matching a mobile number (searching encrypted mobile and username columns).
     *
     * @param mobileNumber the 10-digit mobile number
     * @param stateTenant  the state-level tenant identifier
     * @param requestInfo  UPYOG request context for decryption
     * @return list of decrypted domain {@link User} profiles matching the mobile number
     */
    private List<User> findUsersByMobile(String mobileNumber, String stateTenant, RequestInfo requestInfo) {
        Map<String, User> userMap = new LinkedHashMap<>();

        // 1. Search by mobileNumber
        UserSearchCriteria mobileCriteria = UserSearchCriteria.builder()
                .mobileNumber(mobileNumber)
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .build();
        try {
            UserSearchCriteria encCriteria = encryptionDecryptionUtil.encryptObject(mobileCriteria, UserConstants.USER_ENCRYPTION_KEY, UserSearchCriteria.class);
            List<User> usersByMobile = userRepository.findAll(encCriteria != null ? encCriteria : mobileCriteria);
            if (!CollectionUtils.isEmpty(usersByMobile)) {
                for (User u : usersByMobile) {
                    if (u.getUuid() != null) {
                        userMap.put(u.getUuid(), u);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[MOBILE-AUTH-STRATEGY] Error querying by mobile: {}", e.getMessage());
        }

        // 2. Search by userName (in case first profile used mobileNumber as username)
        UserSearchCriteria userCriteria = UserSearchCriteria.builder()
                .userName(mobileNumber)
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .build();
        try {
            UserSearchCriteria encCriteria = encryptionDecryptionUtil.encryptObject(userCriteria, UserConstants.USER_ENCRYPTION_KEY, UserSearchCriteria.class);
            List<User> usersByUserName = userRepository.findAll(encCriteria != null ? encCriteria : userCriteria);
            if (!CollectionUtils.isEmpty(usersByUserName)) {
                for (User u : usersByUserName) {
                    if (u.getUuid() != null) {
                        userMap.put(u.getUuid(), u);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[MOBILE-AUTH-STRATEGY] Error querying by userName: {}", e.getMessage());
        }

        if (userMap.isEmpty()) {
            return Collections.emptyList();
        }

        return decryptUserList(new ArrayList<>(userMap.values()), requestInfo);
    }

    /**
     * Finds a citizen user by UUID in the specified state tenant.
     *
     * @param uuid        citizen user UUID
     * @param stateTenant state tenant identifier
     * @param requestInfo request info context
     * @return decrypted {@link User} or null if not found
     */
    private User findUserByUuid(String uuid, String stateTenant, RequestInfo requestInfo) {
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .uuid(Collections.singletonList(uuid))
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .build();

        List<User> users = userRepository.findAll(criteria);
        if (CollectionUtils.isEmpty(users)) {
            return null;
        }

        List<User> decrypted = decryptUserList(users, requestInfo);
        return CollectionUtils.isEmpty(decrypted) ? users.get(0) : decrypted.get(0);
    }

    /**
     * Decrypts a list of users using UserSelf or default encryption key.
     *
     * @param users       list of users with encrypted demographic fields
     * @param requestInfo UPYOG RequestInfo context
     * @return list of decrypted users
     */
    private List<User> decryptUserList(List<User> users, RequestInfo requestInfo) {
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyList();
        }
        try {
            List<User> decrypted = encryptionDecryptionUtil.decryptObject(users, UserConstants.USER_SELF_ENCRYPTION_KEY, User.class, requestInfo);
            return !CollectionUtils.isEmpty(decrypted) ? decrypted : users;
        } catch (Exception e) {
            log.warn("[MOBILE-AUTH-STRATEGY] Failed to decrypt user list using UserSelf: {}. Attempting fallback key.", e.getMessage());
            try {
                List<User> decrypted = encryptionDecryptionUtil.decryptObject(users, null, User.class, requestInfo);
                return !CollectionUtils.isEmpty(decrypted) ? decrypted : users;
            } catch (Exception ex) {
                log.warn("[MOBILE-AUTH-STRATEGY] Decryption fallback returned raw user list: {}", ex.getMessage());
                return users;
            }
        }
    }

    /**
     * Resolves tenant ID to state-level tenant if state-level roles are enabled.
     *
     * @param tenantId city or state tenant ID
     * @return state-level tenant identifier
     */
    private String resolveStateTenant(String tenantId) {
        if (isRoleStateLevel() && StringUtils.isNotBlank(tenantId) && tenantId.contains(".")) {
            return tenantId.split("\\.")[0];
        }
        return tenantId;
    }

    /**
     * Masks mobile number for secure log masking.
     *
     * @param mobile 10-digit mobile number
     * @return masked mobile string
     */
    private String maskMobile(String mobile) {
        if (StringUtils.isBlank(mobile)) return "N/A";
        if (mobile.length() >= 6) {
            return mobile.substring(0, 2) + "******" + mobile.substring(mobile.length() - 2);
        }
        return "******";
    }
}
