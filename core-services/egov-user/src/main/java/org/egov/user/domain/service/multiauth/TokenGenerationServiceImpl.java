package org.egov.user.domain.service.multiauth;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.SecureUser;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.UserService;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.auth.Role;
import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.multiauth.LoginV2Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of {@link TokenGenerationService} managing opaque/JWT token creation and Redis session caching.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class TokenGenerationServiceImpl implements TokenGenerationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtEncoder jwtEncoder;
    private final UserService userService;
    private final org.egov.user.config.UserConfiguration userConfiguration;

    /**
     * Primary constructor injecting token storage, encoders, and configuration properties.
     *
     * @param redisTemplate     Redis template for caching OAuth tokens
     * @param jwtEncoder        JWT encoder for signed token generation (if configured)
     * @param userService       core user service for user details
     * @param userConfiguration application properties including token validity TTL
     */
    @Autowired
    public TokenGenerationServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                     @Autowired(required = false) JwtEncoder jwtEncoder,
                                     UserService userService,
                                     @Autowired(required = false) org.egov.user.config.UserConfiguration userConfiguration) {
        this.redisTemplate = redisTemplate;
        this.jwtEncoder = jwtEncoder;
        this.userService = userService;
        this.userConfiguration = userConfiguration;
    }

    /**
     * Testing constructor without userConfiguration.
     *
     * @param redisTemplate Redis template for token storage
     * @param jwtEncoder    JWT encoder for token signing
     * @param userService   core user service
     */
    public TokenGenerationServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                     JwtEncoder jwtEncoder,
                                     UserService userService) {
        this(redisTemplate, jwtEncoder, userService, null);
    }

    private int getAccessTokenValidityInMinutes() {
        return (userConfiguration != null && userConfiguration.getAccessTokenValidityInMinutes() != null)
                ? userConfiguration.getAccessTokenValidityInMinutes()
                : 10080;
    }

    private int getRefreshTokenValidityInMinutes() {
        return (userConfiguration != null && userConfiguration.getRefreshTokenValidityInMinutes() != null)
                ? userConfiguration.getRefreshTokenValidityInMinutes()
                : 20160;
    }

    private String getTokenFormat() {
        return (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getOauth2TokenFormat()))
                ? userConfiguration.getOauth2TokenFormat()
                : "opaque";
    }

    /**
     * Generates access and refresh tokens, converts domain user to contract User,
     * and strictly blocks inactive user accounts from receiving tokens.
     *
     * @param domainUser domain user entity
     * @param scope      OAuth token scope
     * @param authType   channel via which user authenticated
     * @param isNewUser  flag indicating if account was newly provisioned
     * @return login response containing generated tokens and user details
     * @throws CustomException if user is inactive
     */
    @Override
    public LoginV2Response generateTokensAndResponse(User domainUser, String scope, AuthType authType, boolean isNewUser) {
        if (domainUser == null || !Boolean.TRUE.equals(domainUser.getActive())) {
            log.error("[TOKEN-GEN-SERVICE] Cannot mint tokens: user {} (ID: {}) is inactive",
                    domainUser != null ? domainUser.getUsername() : "null",
                    domainUser != null ? domainUser.getId() : "null");
            throw new CustomException(UserConstants.ERROR_ACCOUNT_INACTIVE, UserConstants.MSG_ACCOUNT_INACTIVE);
        }

        String effectiveScope = StringUtils.isNotBlank(scope) ? scope : "read write";

        // 1. Convert domain User to Contract User with full demographic & resync fields
        org.egov.user.web.contract.auth.User contractUser = convertToContractUser(domainUser);
        SecureUser secureUser = new SecureUser(contractUser);

        // 2. Generate Access and Refresh Tokens
        String accessToken = generateAccessToken(secureUser, effectiveScope, contractUser);
        String refreshToken = generateRefreshToken(secureUser, contractUser);

        // 3. Construct Unified Response
        ResponseInfo responseInfo = ResponseInfo.builder()
                .status(String.valueOf(HttpStatus.OK.value()))
                .apiId("egov-user-loginv2")
                .ver("2.0")
                .ts(System.currentTimeMillis())
                .msgId(UUID.randomUUID().toString())
                .resMsgId("uip-loginv2-success")
                .build();

        log.info("[TOKEN-GEN-SERVICE] Minting session tokens for citizen: {} (ID: {}) via channel: {}",
                domainUser.getUsername(), domainUser.getId(), authType);

        return LoginV2Response.builder()
                .responseInfo(responseInfo)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(getAccessTokenValidityInMinutes() * 60)
                .scope(effectiveScope)
                .userRequest(contractUser)
                .authenticatedVia(authType)
                .isNewUser(isNewUser)
                .isProfileCompleted(contractUser != null && Boolean.TRUE.equals(contractUser.getIsProfileCompleted()))
                .build();
    }

    /**
     * Delegates access token generation to either opaque UUID or JWT format based on configuration.
     *
     * @param secureUser security principal
     * @param scope      token permissions scope
     * @param user       contract user model
     * @return generated token string
     */
    private String generateAccessToken(SecureUser secureUser, String scope, org.egov.user.web.contract.auth.User user) {
        if ("opaque".equalsIgnoreCase(getTokenFormat()) || jwtEncoder == null) {
            return generateOpaqueAccessToken(secureUser, scope, user);
        } else {
            return generateJwtAccessToken(secureUser, scope, user);
        }
    }

    /**
     * Generates a random UUID opaque access token, saves session metadata to Redis, and sets TTL.
     *
     * @param secureUser security principal
     * @param scope      token permissions scope
     * @param user       contract user model
     * @return opaque token string
     */
    private String generateOpaqueAccessToken(SecureUser secureUser, String scope, org.egov.user.web.contract.auth.User user) {
        String tokenValue = UUID.randomUUID().toString();
        Map<String, Object> tokenMetadata = new HashMap<>();

        if (user != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("uuid", user.getUuid());
            userInfo.put("userName", user.getUserName());
            userInfo.put("name", user.getName());
            userInfo.put("mobileNumber", user.getMobileNumber());
            userInfo.put("emailId", user.getEmailId());
            userInfo.put("locale", user.getLocale());
            userInfo.put("type", user.getType());
            userInfo.put("tenantId", user.getTenantId());
            userInfo.put("active", user.isActive());
            userInfo.put("fatherName", user.getFatherName());
            userInfo.put("motherName", user.getMotherName());
            userInfo.put("spouseName", user.getSpouseName());
            userInfo.put("isIdentityVerified", user.getIsIdentityVerified());
            userInfo.put("identityVerifiedDate", user.getIdentityVerifiedDate());
            userInfo.put("identityVerifiedSource", user.getIdentityVerifiedSource());
            userInfo.put("lastResyncDate", user.getLastResyncDate());
            userInfo.put("edittedCount", user.getEdittedCount());
            userInfo.put("lastLoginMethod", user.getLastLoginMethod());

            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                List<Map<String, String>> rolesList = user.getRoles().stream()
                        .map(role -> {
                            Map<String, String> roleMap = new HashMap<>();
                            roleMap.put("code", role.getCode());
                            roleMap.put("name", role.getName());
                            roleMap.put("tenantId", role.getTenantId());
                            return roleMap;
                        })
                        .collect(Collectors.toList());
                userInfo.put("roles", rolesList);
            } else {
                userInfo.put("roles", Collections.emptyList());
            }

            tokenMetadata.put("UserRequest", userInfo);
            tokenMetadata.put("userId", user.getId());
            tokenMetadata.put("userName", user.getUserName());
            tokenMetadata.put("userType", user.getType());
            tokenMetadata.put("tenantId", user.getTenantId());
        }

        Map<String, Object> resInfoMap = new LinkedHashMap<>();
        resInfoMap.put("api_id", "egov-user");
        resInfoMap.put("ver", "1.0");
        resInfoMap.put("ts", System.currentTimeMillis());
        resInfoMap.put("status", "Access Token generated successfully");

        tokenMetadata.put("ResponseInfo", resInfoMap);
        tokenMetadata.put("scope", scope);
        tokenMetadata.put("token_type", "access_token");

        // Save in Redis with expiration
        String key = "access_token:" + tokenValue;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, tokenMetadata, getAccessTokenValidityInMinutes(), TimeUnit.MINUTES);
                log.info("Stored opaque access_token in Redis: key={}, expiry={} mins", key, getAccessTokenValidityInMinutes());
            } catch (Exception e) {
                log.warn("Redis unavailable while storing token (fallback for dev without Redis): {}", e.getMessage());
            }
        }

        return tokenValue;
    }

    /**
     * Generates a signed JWT access token containing standard subject/audience claims along with
     * citizen profile details, tenant info, and login method in the token claims payload.
     *
     * @param secureUser the Spring Security user principal
     * @param scope      the authorized OAuth2 scope string
     * @param user       the populated contract user model containing demographic attributes
     * @return signed JWT access token string
     */
    private String generateJwtAccessToken(SecureUser secureUser, String scope, org.egov.user.web.contract.auth.User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(getAccessTokenValidityInMinutes(), ChronoUnit.MINUTES);

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("http://localhost:8080")
                .subject(secureUser.getUsername())
                .audience(Collections.singletonList("egov-user-client"))
                .issuedAt(now)
                .expiresAt(expiry)
                .claim("scope", scope)
                .claim("token_type", "access_token");

        if (user != null) {
            claimsBuilder
                    .claim("userId", user.getId())
                    .claim("userName", user.getUserName())
                    .claim("userType", user.getType())
                    .claim("tenantId", user.getTenantId())
                    .claim("lastLoginMethod", user.getLastLoginMethod())
                    .claim("UserRequest", user);
        }

        JwtClaimsSet claims = claimsBuilder.build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * Generates an opaque random UUID refresh token and persists session metadata in Redis with expiry.
     *
     * @param secureUser the Spring Security user principal
     * @param user       the contract user representation
     * @return opaque refresh token string
     */
    private String generateRefreshToken(SecureUser secureUser, org.egov.user.web.contract.auth.User user) {
        String tokenValue = UUID.randomUUID().toString();
        Map<String, Object> tokenMetadata = new HashMap<>();
        tokenMetadata.put("tokenType", "refresh_token");
        tokenMetadata.put("issuedAt", Instant.now().toEpochMilli());

        if (user != null) {
            tokenMetadata.put("subject", user.getUserName());
            tokenMetadata.put("userId", user.getId());
            tokenMetadata.put("tenantId", user.getTenantId());
        }

        String key = "refresh_token:" + tokenValue;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, tokenMetadata, getRefreshTokenValidityInMinutes(), TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("Redis unavailable while storing refresh token: {}", e.getMessage());
            }
        }
        return tokenValue;
    }

    /**
     * Converts an internal domain User model into the web contract User object, mapping roles,
     * demographic fields, verification status, and timestamps.
     *
     * @param domainUser the internal domain User entity
     * @return contract user representation, or null if input domainUser is null
     */
    private org.egov.user.web.contract.auth.User convertToContractUser(User domainUser) {
        if (domainUser == null) {
            return null;
        }

        Set<Role> authRoles = new HashSet<>();
        if (domainUser.getRoles() != null) {
            for (org.egov.user.domain.model.Role r : domainUser.getRoles()) {
                authRoles.add(new Role(r));
            }
        }

        boolean isProfileComplete = StringUtils.isNotBlank(domainUser.getName())
                && StringUtils.isNotBlank(domainUser.getFatherName())
                && StringUtils.isNotBlank(domainUser.getMotherName())
                && StringUtils.isNotBlank(domainUser.getMobileNumber())
                && domainUser.getDob() != null
                && StringUtils.isNotBlank(domainUser.getEmailId())
                && domainUser.getGender() != null;

        return org.egov.user.web.contract.auth.User.builder()
                .id(domainUser.getId())
                .uuid(domainUser.getUuid())
                .userName(domainUser.getUsername())
                .name(domainUser.getName())
                .mobileNumber(domainUser.getMobileNumber())
                .emailId(domainUser.getEmailId())
                .locale(domainUser.getLocale())
                .active(domainUser.getActive() != null ? domainUser.getActive() : true)
                .type(domainUser.getType() != null ? domainUser.getType().name() : UserType.CITIZEN.name())
                .tenantId(domainUser.getTenantId())
                .fatherName(domainUser.getFatherName())
                .motherName(domainUser.getMotherName())
                .spouseName(domainUser.getSpouseName())
                .isIdentityVerified(domainUser.getIsIdentityVerified())
                .identityVerifiedDate(domainUser.getIdentityVerifiedDate() != null ? domainUser.getIdentityVerifiedDate().getTime() : null)
                .identityVerifiedSource(domainUser.getIdentityVerifiedSource())
                .lastResyncDate(domainUser.getLastResyncDate() != null ? domainUser.getLastResyncDate().getTime() : null)
                .edittedCount(domainUser.getEdittedCount())
                .lastLoginMethod(domainUser.getLastLoginMethod())
                .isProfileCompleted(isProfileComplete)
                .roles(authRoles)
                .build();
    }
}
