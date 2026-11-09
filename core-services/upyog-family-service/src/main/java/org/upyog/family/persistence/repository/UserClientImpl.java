package org.upyog.family.persistence.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.util.MaskUtils;
import org.upyog.family.web.contract.CitizenProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link UserClient} communicating with UPYOG user services to validate and enrich citizen profiles.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class UserClientImpl implements UserClient {

    private final RestTemplate restTemplate;
    private final FamilyConfiguration configuration;

    /**
     * Constructs a UserClientImpl with the given RestTemplate and configuration.
     *
     * @param restTemplate Spring RestTemplate for remote HTTP calls to the user service
     * @param configuration family service configuration containing user endpoints
     */
    @Autowired
    public UserClientImpl(RestTemplate restTemplate, FamilyConfiguration configuration) {
        this.restTemplate = restTemplate;
        this.configuration = configuration;
    }

    /**
     * Queries the UPYOG user service to retrieve citizen profiles by a list of citizen UUIDs.
     *
     * @param uuids list of citizen user UUIDs
     * @param requestInfo UPYOG request context envelope
     * @return map of citizen UUID to CitizenProfile object
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, CitizenProfile> searchUsersByUuids(List<String> uuids, RequestInfo requestInfo) {
        if (uuids == null || uuids.isEmpty()) {
            return Collections.emptyMap();
        }

        if (configuration == null || StringUtils.isBlank(configuration.getUserHost())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "egov.user.host is not configured");
        }
        if (StringUtils.isBlank(configuration.getUserSearchPath())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "egov.user.search.path is not configured");
        }
        String url = buildUrl(configuration.getUserHost(), configuration.getUserSearchPath());
        log.debug("[UPYOG-USER-CLIENT] Querying user service for {} UUIDs from: {}", uuids.size(), url);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("RequestInfo", requestInfo);
        requestBody.put("uuid", uuids);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("user")) {
                List<Map<String, Object>> userList = (List<Map<String, Object>>) response.get("user");
                if (userList != null) {
                    return userList.stream()
                            .map(this::mapToCitizenProfile)
                            .filter(p -> StringUtils.isNotBlank(p.getUuid()))
                            .collect(Collectors.toMap(CitizenProfile::getUuid, p -> p, (existing, replacement) -> existing));
                }
            }
        } catch (Exception e) {
            log.warn("[UPYOG-USER-CLIENT] UPYOG user search failed or unreachable: {}", e.getMessage());
        }

        return Collections.emptyMap();
    }

    /**
     * Searches citizen profiles in the UPYOG user registry by 10-digit mobile number and tenant.
     *
     * @param mobileNumber citizen mobile number
     * @param tenantId tenant identifier
     * @param requestInfo UPYOG request context envelope
     * @return list of matching CitizenProfile objects
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<CitizenProfile> searchUsersByMobile(String mobileNumber, String tenantId, RequestInfo requestInfo) {
        if (StringUtils.isBlank(mobileNumber)) {
            return Collections.emptyList();
        }

        if (configuration == null || StringUtils.isBlank(configuration.getUserHost())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "egov.user.host is not configured");
        }
        if (StringUtils.isBlank(configuration.getUserSearchPath())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "egov.user.search.path is not configured");
        }
        String url = buildUrl(configuration.getUserHost(), configuration.getUserSearchPath());
        log.debug("[UPYOG-USER-CLIENT] Querying user service for mobile: {} from: {}", MaskUtils.maskMobile(mobileNumber), url);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("RequestInfo", requestInfo);
        requestBody.put("mobileNumber", mobileNumber);
        if (StringUtils.isNotBlank(tenantId)) {
            requestBody.put("tenantId", tenantId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("user")) {
                List<Map<String, Object>> userList = (List<Map<String, Object>>) response.get("user");
                if (userList != null) {
                    return userList.stream()
                            .map(this::mapToCitizenProfile)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.warn("[UPYOG-USER-CLIENT] UPYOG user mobile search failed or unreachable: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * Provisions a new citizen profile in the UPYOG user registry for a newly added family member.
     *
     * @param profile citizen profile attributes
     * @param requestInfo UPYOG request context envelope
     * @return created or local CitizenProfile instance with populated UUID
     */
    @Override
    @SuppressWarnings("unchecked")
    public CitizenProfile createCitizenProfile(CitizenProfile profile, RequestInfo requestInfo) {
        if (profile == null) {
            return null;
        }

        if (configuration == null || StringUtils.isBlank(configuration.getUserHost())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "egov.user.host is not configured");
        }
        if (StringUtils.isBlank(configuration.getUserCreatePath())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "egov.user.create.path is not configured");
        }
        String url = buildUrl(configuration.getUserHost(), configuration.getUserCreatePath());
        log.info("[UPYOG-USER-CLIENT] Auto-creating citizen user for mobile: {} at: {}", MaskUtils.maskMobile(profile.getMobileNumber()), url);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("RequestInfo", requestInfo);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", StringUtils.isNotBlank(profile.getName()) ? profile.getName() : "Citizen");
        userMap.put("userName", profile.getMobileNumber());
        userMap.put("mobileNumber", profile.getMobileNumber());
        userMap.put("gender", StringUtils.isNotBlank(profile.getGender()) ? profile.getGender() : "OTHERS");
        userMap.put("type", "CITIZEN");
        userMap.put("active", true);

        requestBody.put("user", userMap);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("user")) {
                List<Map<String, Object>> userList = (List<Map<String, Object>>) response.get("user");
                if (userList != null && !userList.isEmpty()) {
                    return mapToCitizenProfile(userList.get(0));
                }
            }
        } catch (Exception e) {
            log.warn("[UPYOG-USER-CLIENT] Remote citizen creation failed: {}. Provisioning local UUID.", e.getMessage());
        }

        return CitizenProfile.builder()
                .uuid(UUID.randomUUID().toString())
                .name(profile.getName())
                .mobileNumber(profile.getMobileNumber())
                .gender(profile.getGender())
                .build();
    }

    /**
     * Validates that the authenticated caller is identity-verified in the user database.
     * Bypasses internal system users (e.g. SYSTEM_USER) and official employees.
     *
     * @param requestInfo UPYOG request context envelope
     * @return caller's CitizenProfile if verified, or null if system/employee caller
     * @throws CustomException if caller identity is unverified or caller info is missing
     */
    @Override
    public CitizenProfile validateCallerIsVerified(RequestInfo requestInfo) {
        if (requestInfo == null || requestInfo.getUserInfo() == null || StringUtils.isBlank(requestInfo.getUserInfo().getUuid())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "Authenticated caller information (RequestInfo.userInfo.uuid) is required.");
        }

        String callerUuid = requestInfo.getUserInfo().getUuid().trim();
        // Allow internal system users to proceed
        if (FamilyConstants.SYSTEM_USER.equalsIgnoreCase(callerUuid) || "SYSTEM".equalsIgnoreCase(callerUuid)) {
            return null;
        }

        // Allow official employees to proceed
        String userType = requestInfo.getUserInfo().getType();
        if ("EMPLOYEE".equalsIgnoreCase(userType)) {
            return null;
        }

        return validateUserIsVerified(callerUuid, requestInfo);
    }

    /**
     * Validates that a specific citizen user is identity-verified (via Aadhaar/e-KYC) in the database.
     *
     * @param userUuid citizen user UUID
     * @param requestInfo UPYOG request context envelope
     * @return CitizenProfile if verified, or null if system user
     * @throws CustomException if user is not found or isIdentityVerified is not true
     */
    @Override
    public CitizenProfile validateUserIsVerified(String userUuid, RequestInfo requestInfo) {
        if (StringUtils.isBlank(userUuid)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "User UUID is required for identity verification check.");
        }

        if (FamilyConstants.SYSTEM_USER.equalsIgnoreCase(userUuid) || "SYSTEM".equalsIgnoreCase(userUuid)) {
            return null;
        }

        Map<String, CitizenProfile> profileMap = searchUsersByUuids(Collections.singletonList(userUuid), requestInfo);
        CitizenProfile profile = profileMap != null ? profileMap.get(userUuid) : null;
        if (profile == null) {
            log.error("[UPYOG-USER-CLIENT] User not found in database for UUID: {}", userUuid);
            throw new CustomException(FamilyConstants.ERROR_USER_NOT_FOUND,
                    "Citizen profile not found in user registry for UUID: " + userUuid);
        }

        if (!Boolean.TRUE.equals(profile.getIsIdentityVerified())) {
            log.error("[UPYOG-USER-CLIENT] User identity not verified in database for UUID: {}", userUuid);
            throw new CustomException(FamilyConstants.ERROR_USER_NOT_VERIFIED,
                    FamilyConstants.MSG_USER_NOT_VERIFIED);
        }

        return profile;
    }

    /**
     * Maps a raw user map returned by the user service into a strongly-typed CitizenProfile object.
     *
     * @param map raw user attributes map
     * @return populated CitizenProfile instance
     */
    private CitizenProfile mapToCitizenProfile(Map<String, Object> map) {
        Long dob = null;
        Object dobObj = map.get("dob");
        if (dobObj instanceof Number) {
            dob = ((Number) dobObj).longValue();
        }

        Boolean isIdentityVerified = null;
        Object idVerifiedObj = map.get("isIdentityVerified");
        if (idVerifiedObj instanceof Boolean) {
            isIdentityVerified = (Boolean) idVerifiedObj;
        } else if (idVerifiedObj != null) {
            isIdentityVerified = Boolean.valueOf(String.valueOf(idVerifiedObj));
        }

        return CitizenProfile.builder()
                .uuid((String) map.get("uuid"))
                .userName((String) map.get("userName"))
                .name((String) map.get("name"))
                .mobileNumber((String) map.get("mobileNumber"))
                .gender((String) map.get("gender"))
                .dob(dob)
                .photo((String) map.get("photo"))
                .emailId((String) map.get("emailId"))
                .isIdentityVerified(isIdentityVerified)
                .build();
    }

    /**
     * Builds an absolute URL from host and API path segment, normalizing trailing/leading slashes.
     *
     * @param host service host
     * @param path configured path
     * @return normalized absolute URL
     */
    private String buildUrl(String host, String path) {
        String cleanHost = host.trim().endsWith("/") ? host.trim() : host.trim() + "/";
        String cleanPath = path.trim().startsWith("/") ? path.trim().substring(1) : path.trim();
        return cleanHost + cleanPath;
    }
}
