package org.egov.user.domain.service.asset;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.PropertyOtpClient;
import org.egov.user.persistence.repository.PropertyServiceClient;
import org.egov.user.persistence.repository.UserPropertyRepository;
import org.egov.user.util.MaskUtils;
import org.egov.user.web.contract.property.UrbanPropertyConsentValidateRequest;
import org.egov.user.web.contract.property.UrbanPropertyConsentValidateResponse;
import org.egov.user.web.contract.property.UrbanPropertyInfo;
import org.egov.user.web.contract.property.UrbanPropertyOtpRequest;
import org.egov.user.web.contract.property.UrbanPropertyOtpResponse;
import org.egov.user.web.contract.property.UrbanPropertyOtpVerifyRequest;
import org.egov.user.web.contract.property.UrbanPropertyOtpVerifyResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Service implementation managing urban property linking, OTP verification lifecycle,
 * and single-use consent token management.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class UrbanPropertyServiceImpl implements UrbanPropertyService {

    private final PropertyServiceClient propertyServiceClient;
    private final UserPropertyRepository userPropertyRepository;
    private final UrbanPropertyOtpSessionRegistry sessionRegistry;
    private final UrbanPropertyOwnershipValidator ownershipValidator;
    private final PropertyOtpClient propertyOtpClient;
    private final UserConfiguration userConfiguration;

    /**
     * Constructs {@link UrbanPropertyServiceImpl} with required collaborators as non-null dependencies.
     *
     * @param propertyServiceClient client querying property-services for assessments and owners
     * @param userPropertyRepository repository checking existing property linkages in database
     * @param sessionRegistry registry storing OTP challenges and verified consent tokens
     * @param ownershipValidator validator enforcing ownership capacity and linking rules
     * @param propertyOtpClient client managing OTP dispatch and validation
     * @param userConfiguration configuration properties for property OTP and consent policies
     */
    @Autowired
    public UrbanPropertyServiceImpl(PropertyServiceClient propertyServiceClient,
                                    UserPropertyRepository userPropertyRepository,
                                    UrbanPropertyOtpSessionRegistry sessionRegistry,
                                    UrbanPropertyOwnershipValidator ownershipValidator,
                                    PropertyOtpClient propertyOtpClient,
                                    UserConfiguration userConfiguration) {
        this.propertyServiceClient = Objects.requireNonNull(propertyServiceClient, UserConstants.MSG_PROPERTY_SERVICE_CLIENT_REQUIRED);
        this.userPropertyRepository = Objects.requireNonNull(userPropertyRepository, UserConstants.MSG_USER_PROPERTY_REPOSITORY_REQUIRED);
        this.sessionRegistry = Objects.requireNonNull(sessionRegistry, UserConstants.MSG_SESSION_REGISTRY_REQUIRED);
        this.ownershipValidator = Objects.requireNonNull(ownershipValidator, UserConstants.MSG_OWNERSHIP_VALIDATOR_REQUIRED);
        this.propertyOtpClient = Objects.requireNonNull(propertyOtpClient, UserConstants.MSG_PROPERTY_OTP_CLIENT_REQUIRED);
        this.userConfiguration = Objects.requireNonNull(userConfiguration, UserConstants.MSG_USER_CONFIGURATION_REQUIRED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UrbanPropertyOtpResponse sendUrbanPropertyOtp(UrbanPropertyOtpRequest request) {
        if (request == null || StringUtils.isBlank(request.getTenantId()) || StringUtils.isBlank(request.getPropertyId())) {
            throw new CustomException(UserConstants.ERROR_INVALID_PROPERTY_OTP_REQUEST, UserConstants.MSG_PROPERTY_SEARCH_MANDATORY_FIELDS);
        }

        String tenantId = request.getTenantId().trim();
        String propertyId = request.getPropertyId().trim();
        RequestInfo requestInfo = request.getRequestInfo();
        String callerUuid = requestInfo != null && requestInfo.getUserInfo() != null ? requestInfo.getUserInfo().getUuid() : null;

        // Rate limiting check: maximum 3 OTP dispatches per 15 minutes per property (prevents SMS flooding)
        if (!sessionRegistry.isOtpDispatchAllowed(propertyId)) {
            log.warn("[URBAN-PROPERTY-SERVICE] Rate limit exceeded for OTP send on property: {}", propertyId);
            throw new CustomException(UserConstants.ERROR_OTP_RATE_LIMIT_EXCEEDED, UserConstants.MSG_OTP_RATE_LIMIT_EXCEEDED);
        }

        log.info("[URBAN-PROPERTY-SERVICE] Validating property ownership and linkage before sending OTP for ID: {}", propertyId);

        // Use system request info for property service search so owner mobile is unmasked
        RequestInfo systemRequestInfo = createSystemRequestInfo(requestInfo);
        UrbanPropertyInfo property = propertyServiceClient.fetchPropertyById(tenantId, propertyId, systemRequestInfo);
        if (property == null) {
            throw new CustomException(UserConstants.ERROR_PROPERTY_NOT_FOUND, UserConstants.MSG_URBAN_PROPERTY_NOT_FOUND_PREFIX + propertyId);
        }

        List<Map<String, Object>> linkedUsers = userPropertyRepository.findActiveLinkedUsersForUrbanProperty(tenantId, propertyId);
        if (linkedUsers == null) {
            linkedUsers = Collections.emptyList();
        }

        // Enforce ownership rules and resolve destination owner contact via validator (SRP)
        String targetOwnerMobile = ownershipValidator.validateAndResolveTargetOwnerMobile(
                property, linkedUsers, callerUuid, request.getOwnerMobileNumber(), requestInfo);

        // Dispatch OTP via decoupled OTP client (SRP / DIP)
        String referenceId = UserConstants.PREFIX_PROP_REF + UUID.randomUUID().toString().substring(0, 8);
        long ttlMs = userConfiguration.getPropertyOtpReferenceTtlMs() != null ? userConfiguration.getPropertyOtpReferenceTtlMs() : 900000L;
        int maxAttempts = userConfiguration.getPropertyOtpVerifyMaxAttempts() != null ? userConfiguration.getPropertyOtpVerifyMaxAttempts() : 3;

        propertyOtpClient.sendOtp(targetOwnerMobile, tenantId, requestInfo);

        // Record successful dispatch for rate-limiting tracking
        sessionRegistry.recordOtpDispatch(propertyId);

        // Register challenge in session registry with callerUuid
        sessionRegistry.registerChallenge(referenceId, propertyId, tenantId, targetOwnerMobile, callerUuid, ttlMs, maxAttempts);

        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.MSG_STATUS_SUCCESSFUL).build();
        return UrbanPropertyOtpResponse.builder()
                .responseInfo(responseInfo)
                .referenceId(referenceId)
                .propertyId(propertyId)
                .maskedMobileNumber(MaskUtils.maskMobile(targetOwnerMobile))
                .message(UserConstants.MSG_OTP_SENT_SUCCESS)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UrbanPropertyOtpVerifyResponse verifyUrbanPropertyOtp(UrbanPropertyOtpVerifyRequest request) {
        if (request == null || StringUtils.isBlank(request.getPropertyId())
                || StringUtils.isBlank(request.getReferenceId()) || StringUtils.isBlank(request.getOtp())) {
            throw new CustomException(UserConstants.ERROR_INVALID_OTP_VERIFY_REQUEST, UserConstants.MSG_OTP_VERIFY_MANDATORY_FIELDS);
        }

        String tenantId = request.getTenantId() != null ? request.getTenantId().trim() : "";
        String propertyId = request.getPropertyId().trim();
        String referenceId = request.getReferenceId().trim();
        String submittedOtp = request.getOtp().trim();

        log.info("[URBAN-PROPERTY-SERVICE] Verifying OTP for reference: {}, property: {}",
                MaskUtils.maskIdentifier(referenceId), propertyId);

        UrbanPropertyOtpSessionRegistry.OtpChallenge challenge =
                sessionRegistry.resolveActiveChallenge(referenceId, propertyId, tenantId);
        if (challenge == null) {
            log.warn("[URBAN-PROPERTY-SERVICE] No active OTP challenge found or session expired for reference: {}",
                    MaskUtils.maskIdentifier(referenceId));
            throw new CustomException(UserConstants.ERROR_INVALID_OTP_SESSION, UserConstants.MSG_INVALID_OTP_SESSION);
        }

        boolean otpValid = propertyOtpClient.validateOtp(
                challenge.getMobileNumber(), submittedOtp, tenantId, request.getRequestInfo());

        if (!otpValid) {
            challenge.recordFailedAttempt();
            if (challenge.isExhausted()) {
                sessionRegistry.invalidateChallenge(referenceId);
                log.warn("[URBAN-PROPERTY-SERVICE] Reference {} exhausted max OTP attempts", MaskUtils.maskIdentifier(referenceId));
                throw new CustomException(UserConstants.ERROR_MAX_OTP_ATTEMPTS_EXCEEDED,
                        UserConstants.MSG_MAX_OTP_ATTEMPTS_EXCEEDED);
            }
            throw new CustomException(UserConstants.ERROR_INVALID_OTP, UserConstants.MSG_INVALID_OTP_ENTERED);
        }

        // OTP Verified successfully! Invalidate challenge session
        sessionRegistry.invalidateChallenge(referenceId);

        // Generate verified consent token bound to callerUuid and tenantId
        String consentToken = UserConstants.PREFIX_PROP_CONSENT + UUID.randomUUID();
        long ttlMs = userConfiguration.getPropertyConsentTokenTtlMs() != null ? userConfiguration.getPropertyConsentTokenTtlMs() : 900000L;
        long expiresAt = System.currentTimeMillis() + ttlMs;

        sessionRegistry.registerConsentToken(consentToken, propertyId, tenantId, challenge.getMobileNumber(),
                null, challenge.getCallerUuid(), ttlMs);

        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.MSG_STATUS_SUCCESSFUL).build();
        return UrbanPropertyOtpVerifyResponse.builder()
                .responseInfo(responseInfo)
                .verified(true)
                .propertyConsentToken(consentToken)
                .propertyId(propertyId)
                .tenantId(tenantId)
                .expiresAt(expiresAt)
                .message(UserConstants.MSG_PROPERTY_OWNERSHIP_VERIFIED_SUCCESS)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UrbanPropertyConsentValidateResponse validateConsentToken(UrbanPropertyConsentValidateRequest request) {
        if (request == null || StringUtils.isBlank(request.getPropertyId())) {
            throw new CustomException(UserConstants.ERROR_INVALID_CONSENT_VALIDATE_REQUEST, UserConstants.MSG_PROPERTY_ID_MANDATORY);
        }
        boolean valid = validatePropertyConsentToken(request.getPropertyId(), request.getConsentToken());
        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.MSG_STATUS_SUCCESSFUL).build();
        return UrbanPropertyConsentValidateResponse.builder()
                .responseInfo(responseInfo)
                .valid(valid)
                .propertyId(request.getPropertyId())
                .message(valid ? UserConstants.MSG_CONSENT_TOKEN_VALID : UserConstants.MSG_CONSENT_TOKEN_INVALID)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validatePropertyConsentToken(String propertyId, String consentToken) {
        return sessionRegistry.validateConsentToken(propertyId, consentToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean consumeConsentToken(String propertyId, String consentToken, String callerUuid, String tenantId) {
        if (StringUtils.isBlank(propertyId) || StringUtils.isBlank(consentToken)) {
            return false;
        }

        // 1. Consume the token from registry (validates callerUuid and tenantId)
        boolean consumed = sessionRegistry.consumeConsentToken(propertyId, consentToken, callerUuid, tenantId);
        if (!consumed) {
            return false;
        }

        // 2. Re-check linking limits at consumption time to prevent race conditions
        try {
            RequestInfo systemRequestInfo = createSystemRequestInfo(null);
            UrbanPropertyInfo property = propertyServiceClient.fetchPropertyById(tenantId, propertyId, systemRequestInfo);
            if (property != null) {
                List<Map<String, Object>> linkedUsers = userPropertyRepository.findActiveLinkedUsersForUrbanProperty(tenantId, propertyId);
                int linkedCount = linkedUsers != null ? linkedUsers.size() : 0;
                int totalOwners = property.getOwners() != null ? property.getOwners().size() : 1;
                if (!ownershipValidator.isEligibleForLinking(property.getOwnershipCategory(), totalOwners, linkedCount)) {
                    log.warn("[URBAN-PROPERTY-SERVICE] Linking capacity exceeded at consumption time for property {}", propertyId);
                    throw new CustomException(UserConstants.ERROR_PROPERTY_LINKING_LIMIT_EXCEEDED,
                            UserConstants.MSG_PROPERTY_LINKING_LIMIT_EXCEEDED + propertyId);
                }
            }
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.warn("[URBAN-PROPERTY-SERVICE] Non-blocking capacity check warning at consumption for property {}: {}",
                    propertyId, e.getMessage());
        }

        return true;
    }

    private RequestInfo createSystemRequestInfo(RequestInfo callerRequestInfo) {
        User systemUser = User.builder()
                .uuid(UserConstants.SYSTEM_USER_UUID)
                .type(UserConstants.USER_TYPE_SYSTEM)
                .roles(Collections.singletonList(Role.builder()
                        .name(UserConstants.ROLE_INTERNAL_SYSTEM)
                        .code(UserConstants.ROLE_INTERNAL_SYSTEM)
                        .build()))
                .build();
        return RequestInfo.builder()
                .apiId(callerRequestInfo != null ? callerRequestInfo.getApiId() : UserConstants.DEFAULT_API_ID)
                .ver(callerRequestInfo != null ? callerRequestInfo.getVer() : UserConstants.DEFAULT_VER)
                .action(callerRequestInfo != null ? callerRequestInfo.getAction() : UserConstants.ACTION_SEARCH)
                .did(callerRequestInfo != null ? callerRequestInfo.getDid() : UserConstants.DEFAULT_DID)
                .key(callerRequestInfo != null ? callerRequestInfo.getKey() : UserConstants.EMPTY_STRING)
                .msgId(callerRequestInfo != null ? callerRequestInfo.getMsgId() : UUID.randomUUID().toString())
                .userInfo(systemUser)
                .build();
    }
}
