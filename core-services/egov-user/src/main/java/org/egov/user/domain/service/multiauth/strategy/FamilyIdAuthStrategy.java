package org.egov.user.domain.service.multiauth.strategy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.UserService;
import org.egov.user.persistence.repository.OtpRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.Otp;
import org.egov.user.web.contract.OtpValidateRequest;
import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.multiauth.LoginV2Request;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication strategy for Family ID and member OTP resolution.
 *
 * <p>Validates the member's OTP under the given family unit, resolves the associated
 * individual citizen profile, or provisions a new lightweight citizen account.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class FamilyIdAuthStrategy implements MultiAuthStrategy {

    private final UserService userService;
    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final EncryptionDecryptionUtil encryptionDecryptionUtil;

    private final org.egov.user.config.UserConfiguration userConfiguration;

    @Autowired
    public FamilyIdAuthStrategy(UserService userService,
                                UserRepository userRepository,
                                OtpRepository otpRepository,
                                EncryptionDecryptionUtil encryptionDecryptionUtil,
                                @Autowired(required = false) org.egov.user.config.UserConfiguration userConfiguration) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.encryptionDecryptionUtil = encryptionDecryptionUtil;
        this.userConfiguration = userConfiguration;
    }

    public FamilyIdAuthStrategy(UserService userService,
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

    @Override
    public boolean supports(AuthType authType) {
        return AuthType.FAMILY_ID.equals(authType);
    }

    @Override
    public boolean canHandle(LoginV2Request request) {
        return request != null && StringUtils.isNotBlank(request.getFamilyId());
    }

    @Override
    public AuthenticatedUserResult authenticateAndResolveUser(LoginV2Request request) {
        String familyId = request.getFamilyId();
        String mobileNumber = request.getMobileNumber();
        String otp = request.getOtp();
        String tenantId = request.getTenantId();

        if (StringUtils.isBlank(familyId)) {
            log.error("[FAMILY-AUTH-STRATEGY] Validation failed: familyId is blank");
            throw new CustomException(UserConstants.ERROR_INVALID_FAMILY_ID, UserConstants.MSG_FAMILY_ID_REQUIRED);
        }
        if (StringUtils.isBlank(mobileNumber)) {
            log.error("[FAMILY-AUTH-STRATEGY] Validation failed: member mobileNumber is blank for family: {}", familyId);
            throw new CustomException(UserConstants.ERROR_INVALID_MOBILE_NUMBER, UserConstants.MSG_FAMILY_MEMBER_MOBILE_REQUIRED);
        }
        if (StringUtils.isBlank(otp)) {
            log.error("[FAMILY-AUTH-STRATEGY] Validation failed: OTP is blank for family: {}", familyId);
            throw new CustomException(UserConstants.ERROR_INVALID_OTP, UserConstants.MSG_LOGIN_OTP_REQUIRED);
        }

        log.info("[FAMILY-AUTH-STRATEGY] Initiating family login: familyId={}, mobile={}, tenant={}",
                familyId, maskMobile(mobileNumber), tenantId);

        // 1. Validate OTP for member mobile number
        validateOtp(mobileNumber, otp, tenantId, request);

        // 2. Resolve member user or auto-create
        String stateTenant = resolveStateTenant(tenantId);

        // 2a. If client specified a selected user UUID (from multi-profile selection), resolve that user directly
        if (StringUtils.isNotBlank(request.getSelectedUserUuid())) {
            log.info("[FAMILY-AUTH-STRATEGY] Resolving selected user UUID: {} for member mobile: {}",
                    request.getSelectedUserUuid(), maskMobile(mobileNumber));
            User selectedUser = findUserByUuid(request.getSelectedUserUuid(), stateTenant, request.getRequestInfo());
            if (selectedUser == null) {
                log.error("[FAMILY-AUTH-STRATEGY] Selected citizen profile not found for UUID: {}", request.getSelectedUserUuid());
                throw new CustomException(UserConstants.ERROR_USER_NOT_FOUND, UserConstants.MSG_SELECTED_USER_NOT_FOUND);
            }
            selectedUser.setLastLoginMethod(AuthType.FAMILY_ID.name());
            return AuthenticatedUserResult.builder()
                    .user(selectedUser)
                    .isNewUser(false)
                    .authType(AuthType.FAMILY_ID)
                    .build();
        }

        // 2b. Query all citizen accounts associated with this member mobile number in this state tenant
        log.debug("[FAMILY-AUTH-STRATEGY] Querying citizen profiles for member mobile: {} in tenant: {}", maskMobile(mobileNumber), stateTenant);
        List<User> matchedUsers = findUsersByMobile(mobileNumber, stateTenant, request.getRequestInfo());

        // 2c. Edge Case: If 2 or more users are linked to same mobile number, return list for user selection
        if (!CollectionUtils.isEmpty(matchedUsers) && matchedUsers.size() > 1) {
            log.info("[FAMILY-AUTH-STRATEGY] Found {} citizen profiles linked to member mobile: {}. Returning list for selection.",
                    matchedUsers.size(), maskMobile(mobileNumber));
            return AuthenticatedUserResult.builder()
                    .multipleUsersFound(true)
                    .matchedUsers(matchedUsers)
                    .authType(AuthType.FAMILY_ID)
                    .build();
        }

        // 2d. If exactly 1 user found, proceed with login
        if (!CollectionUtils.isEmpty(matchedUsers) && matchedUsers.size() == 1) {
            User user = matchedUsers.get(0);
            log.info("[FAMILY-AUTH-STRATEGY] Matched single citizen ID: {}, UUID: {} for familyId: {}",
                    user.getId(), user.getUuid(), familyId);
            user.setLastLoginMethod(AuthType.FAMILY_ID.name());
            return AuthenticatedUserResult.builder()
                    .user(user)
                    .isNewUser(false)
                    .authType(AuthType.FAMILY_ID)
                    .build();
        }

        // 2e. Auto-create citizen profile for family member if not found
        log.info("[FAMILY-AUTH-STRATEGY] Member account not found for mobile: {}. Auto-provisioning citizen profile.",
                maskMobile(mobileNumber));
        String name = StringUtils.isNotBlank(request.getName()) ? request.getName().trim() : "Family Member";

        User newUser = User.builder()
                .username(mobileNumber)
                .name(name)
                .mobileNumber(mobileNumber)
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .active(true)
                .lastLoginMethod(AuthType.FAMILY_ID.name())
                .roles(Collections.singleton(Role.getCitizenRole()))
                .otpValidationMandatory(false)
                .build();

        User createdUser = userService.createUser(newUser, request.getRequestInfo());
        log.info("[FAMILY-AUTH-STRATEGY] Successfully provisioned new family member citizen ID: {}, UUID: {}",
                createdUser.getId(), createdUser.getUuid());

        return AuthenticatedUserResult.builder()
                .user(createdUser)
                .isNewUser(true)
                .authType(AuthType.FAMILY_ID)
                .build();
    }

    /**
     * Validates member OTP against egov-otp service or configured fixed test OTP.
     *
     * @param mobileNumber the member's 10-digit mobile number
     * @param otp          entered OTP string
     * @param tenantId     tenant identifier
     * @param request      authentication request containing RequestInfo
     * @throws CustomException if OTP verification is rejected
     */
    private void validateOtp(String mobileNumber, String otp, String tenantId, LoginV2Request request) {
        log.debug("[FAMILY-AUTH-STRATEGY] Validating member OTP for mobile: {}", maskMobile(mobileNumber));

        if (isFixedOTPEnabled() && StringUtils.isNotBlank(getFixedOTPPassword()) && getFixedOTPPassword().equals(otp)) {
            log.info("[FAMILY-AUTH-STRATEGY] Fixed OTP matched for family member mobile: {}", maskMobile(mobileNumber));
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
            log.error("[FAMILY-AUTH-STRATEGY] OTP verification rejected by egov-otp for member mobile: {}", maskMobile(mobileNumber));
            throw new CustomException(UserConstants.ERROR_INVALID_OTP, "Invalid or expired OTP for family member: " + maskMobile(mobileNumber));
        }
        log.info("[FAMILY-AUTH-STRATEGY] OTP verified successfully for family member mobile: {}", maskMobile(mobileNumber));
    }

    /**
     * Resolves an individual citizen profile by UUID in the state tenant, decrypting sensitive attributes.
     *
     * @param uuid         the citizen unique identifier
     * @param stateTenant  the state-level tenant identifier
     * @param requestInfo  UPYOG request context for decryption
     * @return decrypted domain {@link User} profile or null if not found
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
        User user = users.get(0);
        return encryptionDecryptionUtil.decryptObject(user, UserConstants.USER_ENCRYPTION_KEY, User.class, requestInfo);
    }

    /**
     * Resolves all citizen users matching a member mobile number (searching encrypted mobile and username columns).
     *
     * @param mobileNumber the 10-digit member mobile number
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
            log.warn("[FAMILY-AUTH-STRATEGY] Error querying member by mobile: {}", e.getMessage());
        }

        // 2. Search by userName (in case profile used mobileNumber as username)
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
            log.warn("[FAMILY-AUTH-STRATEGY] Error querying member by userName: {}", e.getMessage());
        }

        // Decrypt all matched domain User models
        List<User> result = new ArrayList<>();
        for (User u : userMap.values()) {
            try {
                User decrypted = encryptionDecryptionUtil.decryptObject(u, UserConstants.USER_ENCRYPTION_KEY, User.class, requestInfo);
                result.add(decrypted != null ? decrypted : u);
            } catch (Exception e) {
                log.warn("[FAMILY-AUTH-STRATEGY] Failed to decrypt citizen record UUID: {}", u.getUuid());
                result.add(u);
            }
        }
        return result;
    }

    /**
     * Resolves the state-level tenant ID when state-level roles are enabled.
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
     * Masks the member's mobile number for log security.
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
