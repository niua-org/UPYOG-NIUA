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

import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private final RestTemplate restTemplate;

    private final org.egov.user.config.UserConfiguration userConfiguration;
    private final Map<String, Integer> invalidOtpAttempts = new ConcurrentHashMap<>();

    @Autowired
    public FamilyIdAuthStrategy(UserService userService,
                                UserRepository userRepository,
                                OtpRepository otpRepository,
                                EncryptionDecryptionUtil encryptionDecryptionUtil,
                                @Autowired(required = false) RestTemplate restTemplate,
                                @Autowired(required = false) org.egov.user.config.UserConfiguration userConfiguration) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.encryptionDecryptionUtil = encryptionDecryptionUtil;
        this.restTemplate = restTemplate;
        this.userConfiguration = userConfiguration;
    }

    public FamilyIdAuthStrategy(UserService userService,
                                UserRepository userRepository,
                                OtpRepository otpRepository,
                                EncryptionDecryptionUtil encryptionDecryptionUtil,
                                org.egov.user.config.UserConfiguration userConfiguration) {
        this(userService, userRepository, otpRepository, encryptionDecryptionUtil, null, userConfiguration);
    }

    public FamilyIdAuthStrategy(UserService userService,
                                UserRepository userRepository,
                                OtpRepository otpRepository,
                                EncryptionDecryptionUtil encryptionDecryptionUtil) {
        this(userService, userRepository, otpRepository, encryptionDecryptionUtil, null, null);
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
        if (StringUtils.isBlank(otp)) {
            log.error("[FAMILY-AUTH-STRATEGY] Validation failed: OTP is blank for family: {}", familyId);
            throw new CustomException(UserConstants.ERROR_INVALID_OTP, UserConstants.MSG_LOGIN_OTP_REQUIRED);
        }

        // Determine effective identifier for OTP and user resolution: mobileNumber OR aadhaarNumber OR familyId
        String effectiveIdentifier;
        if (StringUtils.isNotBlank(mobileNumber)) {
            effectiveIdentifier = mobileNumber;
        } else if (StringUtils.isNotBlank(request.getAadhaarNumber())) {
            effectiveIdentifier = request.getAadhaarNumber();
        } else {
            effectiveIdentifier = familyId;
        }
        log.info("[FAMILY-AUTH-STRATEGY] Initiating family login: familyId={}, mobile={}, aadhaar={}, effectiveIdentifier={}, tenant={}",
                familyId, maskMobile(mobileNumber), maskMobile(request.getAadhaarNumber()), maskMobile(effectiveIdentifier), tenantId);

        // 1. Validate OTP for member mobile number or familyId
        validateOtp(effectiveIdentifier, otp, tenantId, request);

        // 2. Resolve member user or auto-create
        String stateTenant = resolveStateTenant(tenantId);

        // 2a. Query citizen accounts associated with verified identifier (mobile or familyId)
        List<User> matchedUsers = Collections.emptyList();
        if (StringUtils.isNotBlank(mobileNumber)) {
            matchedUsers = findUsersByMobile(mobileNumber, stateTenant, request.getRequestInfo());
        }
        if (CollectionUtils.isEmpty(matchedUsers) && StringUtils.isNotBlank(familyId)) {
            matchedUsers = findUsersByUserName(familyId, stateTenant, request.getRequestInfo());
        }

        // 2b. If client specified a selected user UUID, verify that it belongs to the matched accounts for this verified identifier
        if (StringUtils.isNotBlank(request.getSelectedUserUuid())) {
            log.info("[FAMILY-AUTH-STRATEGY] Resolving selected user UUID: {} for effective identifier: {}",
                    request.getSelectedUserUuid(), maskMobile(effectiveIdentifier));
            User selectedUser = matchedUsers.stream()
                    .filter(u -> request.getSelectedUserUuid().equals(u.getUuid()))
                    .findFirst()
                    .orElse(null);

            if (selectedUser == null) {
                log.error("[FAMILY-AUTH-STRATEGY] Selected citizen profile not found or does not belong to verified identifier: {}", request.getSelectedUserUuid());
                throw new CustomException(UserConstants.ERROR_USER_NOT_FOUND, UserConstants.MSG_SELECTED_USER_NOT_FOUND);
            }
            selectedUser.setLastLoginMethod(AuthType.FAMILY_ID.name());
            triggerFamilyJitProvision(selectedUser, request);
            return AuthenticatedUserResult.builder()
                    .user(selectedUser)
                    .isNewUser(false)
                    .authType(AuthType.FAMILY_ID)
                    .build();
        }

        // 2c. Edge Case: If 2 or more users are linked to same identifier, return list for user selection
        if (!CollectionUtils.isEmpty(matchedUsers) && matchedUsers.size() > 1) {
            log.info("[FAMILY-AUTH-STRATEGY] Found {} citizen profiles linked to effective identifier: {}. Returning list for selection.",
                    matchedUsers.size(), maskMobile(effectiveIdentifier));
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
            triggerFamilyJitProvision(user, request);
            return AuthenticatedUserResult.builder()
                    .user(user)
                    .isNewUser(false)
                    .authType(AuthType.FAMILY_ID)
                    .build();
        }

        // 2e. Auto-create citizen profile for family member / head if not found
        log.info("[FAMILY-AUTH-STRATEGY] Citizen account not found for identifier: {}. Auto-provisioning citizen profile.",
                maskMobile(effectiveIdentifier));
        String defaultName = StringUtils.isNotBlank(mobileNumber) ? "Family Member"
                : (StringUtils.isNotBlank(request.getAadhaarNumber()) ? "Aadhaar Citizen" : "Family Citizen");
        String name = StringUtils.isNotBlank(request.getName()) ? request.getName().trim() : defaultName;

        User newUser = User.builder()
                .username(effectiveIdentifier)
                .name(name)
                .mobileNumber(StringUtils.isNotBlank(mobileNumber) ? mobileNumber : null)
                .aadhaarNumber(StringUtils.isNotBlank(request.getAadhaarNumber()) ? request.getAadhaarNumber() : null)
                .isIdentityVerified(StringUtils.isNotBlank(request.getAadhaarNumber()))
                .identityVerifiedDate(StringUtils.isNotBlank(request.getAadhaarNumber()) ? new Date() : null)
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .active(true)
                .lastLoginMethod(AuthType.FAMILY_ID.name())
                .roles(Collections.singleton(Role.getCitizenRole()))
                .otpValidationMandatory(false)
                .build();

        User createdUser = userService.createUser(newUser, request.getRequestInfo());
        log.info("[FAMILY-AUTH-STRATEGY] Successfully provisioned new family citizen ID: {}, UUID: {}",
                createdUser.getId(), createdUser.getUuid());

        triggerFamilyJitProvision(createdUser, request);
        return AuthenticatedUserResult.builder()
                .user(createdUser)
                .isNewUser(true)
                .authType(AuthType.FAMILY_ID)
                .build();
    }

    /**
     * Triggers JIT family existence check and member mapping in upyog-family-service.
     * Ensures family aggregate is created and member is mapped upon successful OTP verification.
     *
     * @param user    authenticated citizen domain user
     * @param request login request containing referenceId / familyId
     */
    private void triggerFamilyJitProvision(User user, LoginV2Request request) {
        if (user == null || (StringUtils.isBlank(request.getFamilyId()) && StringUtils.isBlank(request.getReferenceId()))) {
            return;
        }
        if (restTemplate == null) {
            log.debug("[FAMILY-AUTH-STRATEGY] RestTemplate not configured, skipping JIT family notification");
            return;
        }
        try {
            String host = (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getFamilyServiceHost()))
                    ? userConfiguration.getFamilyServiceHost()
                    : "http://localhost:8092/";
            String endpoint = (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getFamilyServiceStateJitEndpoint()))
                    ? userConfiguration.getFamilyServiceStateJitEndpoint()
                    : "/family/v1/state/_jitProvision";

            String url = host + (endpoint.startsWith("/") ? endpoint.substring(1) : endpoint);

            Map<String, Object> jitRequest = new HashMap<>();
            jitRequest.put("RequestInfo", request.getRequestInfo());
            jitRequest.put("referenceId", request.getReferenceId());
            jitRequest.put("stateFamilyId", request.getFamilyId());
            jitRequest.put("tenantId", request.getTenantId());
            jitRequest.put("userUuid", user.getUuid());
            jitRequest.put("selectedMemberId", request.getSelectedMemberId());
            jitRequest.put("mobileNumber", request.getMobileNumber());
            jitRequest.put("aadhaarNumber", request.getAadhaarNumber());

            log.info("[FAMILY-AUTH-STRATEGY] Triggering JIT Family & Member check in upyog-family-service at {}: familyId={}, userUuid={}",
                    url, request.getFamilyId(), user.getUuid());

            restTemplate.postForObject(url, jitRequest, Map.class);
            log.info("[FAMILY-AUTH-STRATEGY] JIT Family check & member mapping completed successfully for user: {}", user.getUuid());
        } catch (HttpStatusCodeException e) {
            log.error("[FAMILY-AUTH-STRATEGY] upyog-family-service rejected JIT provision with status {}: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            String errorCode = UserConstants.ERROR_INVALID_REQUEST;
            String errorMessage = "User already belongs to another active family";
            try {
                JsonNode root = new ObjectMapper().readTree(e.getResponseBodyAsString());
                JsonNode errors = root.get("Errors");
                if (errors != null && errors.isArray() && errors.size() > 0) {
                    JsonNode firstError = errors.get(0);
                    if (firstError.has("code")) errorCode = firstError.get("code").asText();
                    if (firstError.has("message")) errorMessage = firstError.get("message").asText();
                }
            } catch (Exception parseEx) {
                log.warn("[FAMILY-AUTH-STRATEGY] Could not parse error response from family service: {}", parseEx.getMessage());
            }
            throw new CustomException(errorCode, errorMessage);
        } catch (Exception e) {
            log.warn("[FAMILY-AUTH-STRATEGY] Non-blocking: upyog-family-service JIT notification encountered: {}", e.getMessage());
        }
    }

    /**
     * Validates OTP for the specified identity (mobile number or family ID) against egov-otp service or configured fixed test OTP.
     *
     * @param identity the member's 10-digit mobile number or family ID
     * @param otp      entered OTP string
     * @param tenantId tenant identifier
     * @param request  authentication request containing RequestInfo
     * @throws CustomException if OTP verification is rejected
     */
    private void validateOtp(String identity, String otp, String tenantId, LoginV2Request request) {
        log.debug("[FAMILY-AUTH-STRATEGY] Validating OTP for identity: {}", maskMobile(identity));

        if (StringUtils.isNotBlank(identity)) {
            int currentAttempts = invalidOtpAttempts.getOrDefault(identity, 0);
            if (currentAttempts >= getMaxOtpAttempts()) {
                log.error("[FAMILY-AUTH-STRATEGY] Locked: identity {} has exceeded maximum allowed OTP attempts ({})",
                        maskMobile(identity), getMaxOtpAttempts());
                throw new CustomException(UserConstants.ERROR_MAX_OTP_ATTEMPTS_EXCEEDED, UserConstants.MSG_MAX_OTP_ATTEMPTS_EXCEEDED);
            }
        }

        if (isFixedOTPEnabled() && StringUtils.isNotBlank(getFixedOTPPassword()) && getFixedOTPPassword().equals(otp)) {
            log.info("[FAMILY-AUTH-STRATEGY] Fixed OTP matched for family identity: {}", maskMobile(identity));
            if (StringUtils.isNotBlank(identity)) {
                invalidOtpAttempts.remove(identity);
            }
            return;
        }

        Otp otpContract = Otp.builder()
                .otp(otp)
                .identity(identity)
                .tenantId(tenantId)
                .build();

        OtpValidateRequest otpValidateRequest = OtpValidateRequest.builder()
                .requestInfo(request.getRequestInfo())
                .otp(otpContract)
                .build();

        boolean isOtpValid = otpRepository.validateOtp(otpValidateRequest);
        if (!isOtpValid) {
            if (StringUtils.isNotBlank(identity)) {
                int attempts = invalidOtpAttempts.compute(identity, (k, v) -> (v == null ? 1 : v + 1));
                if (attempts >= getMaxOtpAttempts()) {
                    log.error("[FAMILY-AUTH-STRATEGY] Identity {} reached max invalid OTP attempts ({}). Lockout triggered.",
                            maskMobile(identity), getMaxOtpAttempts());
                    throw new CustomException(UserConstants.ERROR_MAX_OTP_ATTEMPTS_EXCEEDED, UserConstants.MSG_MAX_OTP_ATTEMPTS_EXCEEDED);
                }
            }
            log.error("[FAMILY-AUTH-STRATEGY] OTP verification rejected by egov-otp for identity: {}", maskMobile(identity));
            throw new CustomException(UserConstants.ERROR_INVALID_OTP, "Invalid or expired OTP for family identity: " + maskMobile(identity));
        }

        if (StringUtils.isNotBlank(identity)) {
            invalidOtpAttempts.remove(identity);
        }
        log.info("[FAMILY-AUTH-STRATEGY] OTP verified successfully for family identity: {}", maskMobile(identity));
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
     * Resolves citizen profiles matching a username (such as the State Family ID).
     *
     * @param userName    the username or family ID string
     * @param stateTenant the state-level tenant identifier
     * @param requestInfo UPYOG request context for decryption
     * @return list of decrypted domain {@link User} profiles matching the username
     */
    private List<User> findUsersByUserName(String userName, String stateTenant, RequestInfo requestInfo) {
        if (StringUtils.isBlank(userName)) {
            return Collections.emptyList();
        }
        UserSearchCriteria userCriteria = UserSearchCriteria.builder()
                .userName(userName)
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .build();
        try {
            UserSearchCriteria encCriteria = encryptionDecryptionUtil.encryptObject(userCriteria, UserConstants.USER_ENCRYPTION_KEY, UserSearchCriteria.class);
            List<User> usersByUserName = userRepository.findAll(encCriteria != null ? encCriteria : userCriteria);
            if (CollectionUtils.isEmpty(usersByUserName)) {
                return Collections.emptyList();
            }
            List<User> result = new ArrayList<>();
            for (User u : usersByUserName) {
                try {
                    User decrypted = encryptionDecryptionUtil.decryptObject(u, UserConstants.USER_ENCRYPTION_KEY, User.class, requestInfo);
                    result.add(decrypted != null ? decrypted : u);
                } catch (Exception e) {
                    log.warn("[FAMILY-AUTH-STRATEGY] Failed to decrypt citizen record UUID: {}", u.getUuid());
                    result.add(u);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("[FAMILY-AUTH-STRATEGY] Error querying citizen by userName: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Resolves citizen profiles matching an Aadhaar number.
     *
     * @param aadhaarNumber the 12-digit plain or masked Aadhaar number
     * @param stateTenant   the state-level tenant identifier
     * @param requestInfo   UPYOG request context for decryption
     * @return list of decrypted domain User profiles matching the Aadhaar number
     */
    private List<User> findUsersByAadhaar(String aadhaarNumber, String stateTenant, RequestInfo requestInfo) {
        if (StringUtils.isBlank(aadhaarNumber)) {
            return Collections.emptyList();
        }
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .aadhaarNumber(aadhaarNumber)
                .tenantId(stateTenant)
                .type(UserType.CITIZEN)
                .build();
        try {
            UserSearchCriteria encCriteria = encryptionDecryptionUtil.encryptObject(criteria, UserConstants.USER_ENCRYPTION_KEY, UserSearchCriteria.class);
            List<User> users = userRepository.findAll(encCriteria != null ? encCriteria : criteria);
            if (!CollectionUtils.isEmpty(users)) {
                List<User> decrypted = new ArrayList<>();
                for (User u : users) {
                    try {
                        User dec = encryptionDecryptionUtil.decryptObject(u, UserConstants.USER_ENCRYPTION_KEY, User.class, requestInfo);
                        decrypted.add(dec != null ? dec : u);
                    } catch (Exception ex) {
                        decrypted.add(u);
                    }
                }
                return decrypted;
            }
        } catch (Exception e) {
            log.warn("[FAMILY-AUTH-STRATEGY] Error querying citizen by Aadhaar: {}", e.getMessage());
        }
        return Collections.emptyList();
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
