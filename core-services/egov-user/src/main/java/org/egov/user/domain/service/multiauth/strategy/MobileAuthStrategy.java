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

    public MobileAuthStrategy(UserService userService,
                              UserRepository userRepository,
                              OtpRepository otpRepository,
                              EncryptionDecryptionUtil encryptionDecryptionUtil) {
        this(userService, userRepository, otpRepository, encryptionDecryptionUtil, null);
    }

    private boolean isFixedOTPEnabled() {
        return userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getCitizenLoginPasswordOtpFixedEnabled());
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

    @Override
    public boolean supports(AuthType authType) {
        return AuthType.MOBILE.equals(authType);
    }

    @Override
    public boolean canHandle(LoginV2Request request) {
        return request != null
                && StringUtils.isNotBlank(request.getMobileNumber())
                && StringUtils.isBlank(request.getFamilyId())
                && StringUtils.isBlank(request.getAadhaarNumber())
                && StringUtils.isBlank(request.getTxnId())
                && StringUtils.isBlank(request.getReferenceId());
    }

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
            selectedUser.setLastLoginMethod(AuthType.MOBILE.name());
            return AuthenticatedUserResult.builder()
                    .user(selectedUser)
                    .isNewUser(false)
                    .authType(AuthType.MOBILE)
                    .build();
        }

        // 2c. Edge Case: If 2 or more users are linked to same mobile number, return list for user selection
        if (!CollectionUtils.isEmpty(matchedUsers) && matchedUsers.size() > 1) {
            log.info("[MOBILE-AUTH-STRATEGY] Found {} citizen profiles linked to mobile: {}. Returning list for selection.",
                    matchedUsers.size(), maskMobile(mobileNumber));
            return AuthenticatedUserResult.builder()
                    .multipleUsersFound(true)
                    .matchedUsers(matchedUsers)
                    .authType(AuthType.MOBILE)
                    .build();
        }

        // 2d. If exactly 1 user found, proceed with login
        if (!CollectionUtils.isEmpty(matchedUsers) && matchedUsers.size() == 1) {
            User user = matchedUsers.get(0);
            log.info("[MOBILE-AUTH-STRATEGY] Matched single citizen ID: {}, UUID: {} for mobile: {}",
                    user.getId(), user.getUuid(), maskMobile(mobileNumber));
            user.setLastLoginMethod(AuthType.MOBILE.name());
            return AuthenticatedUserResult.builder()
                    .user(user)
                    .isNewUser(false)
                    .authType(AuthType.MOBILE)
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
                .lastLoginMethod(AuthType.MOBILE.name())
                .roles(Collections.singleton(Role.getCitizenRole()))
                .otpValidationMandatory(false)
                .build();

        User createdUser = userService.createUser(newUser, request.getRequestInfo());
        log.info("[MOBILE-AUTH-STRATEGY] Successfully provisioned new citizen ID: {}, UUID: {}",
                createdUser.getId(), createdUser.getUuid());

        return AuthenticatedUserResult.builder()
                .user(createdUser)
                .isNewUser(true)
                .authType(AuthType.MOBILE)
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
