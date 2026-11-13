package org.upyog.family.persistence.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.util.MaskUtils;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.FamilyMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_USER_HOST_NOT_CONFIGURED);
        }
        if (StringUtils.isBlank(configuration.getUserSearchPath())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_USER_SEARCH_PATH_NOT_CONFIGURED);
        }
        String url = buildUrl(configuration.getUserHost(), configuration.getUserSearchPath());
        log.debug("[UPYOG-USER-CLIENT] Querying user service for {} UUIDs from: {}", uuids.size(), url);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(FamilyConstants.JSON_KEY_REQUEST_INFO, requestInfo);
        requestBody.put(FamilyConstants.JSON_KEY_UUID, uuids);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Explicitly ensure inter-service context by omitting or removing gateway pass-through header
        headers.remove("x-pass-through-gateway");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey(FamilyConstants.JSON_KEY_USER)) {
                List<Map<String, Object>> userList = (List<Map<String, Object>>) response.get(FamilyConstants.JSON_KEY_USER);
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
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_USER_HOST_NOT_CONFIGURED);
        }
        if (StringUtils.isBlank(configuration.getUserSearchPath())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_USER_SEARCH_PATH_NOT_CONFIGURED);
        }
        String url = buildUrl(configuration.getUserHost(), configuration.getUserSearchPath());
        log.debug("[UPYOG-USER-CLIENT] Querying user service for mobile: {} from: {}", MaskUtils.maskMobile(mobileNumber), url);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(FamilyConstants.JSON_KEY_REQUEST_INFO, requestInfo);
        requestBody.put(FamilyConstants.JSON_KEY_MOBILE_NUMBER, mobileNumber);
        if (StringUtils.isNotBlank(tenantId)) {
            requestBody.put(FamilyConstants.PARAM_TENANT_ID, tenantId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Explicitly ensure inter-service context by omitting or removing gateway pass-through header
        headers.remove("x-pass-through-gateway");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey(FamilyConstants.JSON_KEY_USER)) {
                List<Map<String, Object>> userList = (List<Map<String, Object>>) response.get(FamilyConstants.JSON_KEY_USER);
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
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_USER_HOST_NOT_CONFIGURED);
        }
        if (StringUtils.isBlank(configuration.getUserCreatePath())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_USER_CREATE_PATH_NOT_CONFIGURED);
        }
        String url = buildUrl(configuration.getUserHost(), configuration.getUserCreatePath());
        log.info("[UPYOG-USER-CLIENT] Auto-creating citizen user for mobile: {} at: {}", MaskUtils.maskMobile(profile.getMobileNumber()), url);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(FamilyConstants.JSON_KEY_REQUEST_INFO, requestInfo);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put(FamilyConstants.JSON_KEY_NAME, StringUtils.isNotBlank(profile.getName()) ? profile.getName() : FamilyConstants.DEFAULT_CITIZEN_NAME);
        userMap.put(FamilyConstants.JSON_KEY_USER_NAME, profile.getMobileNumber());
        userMap.put(FamilyConstants.JSON_KEY_MOBILE_NUMBER, profile.getMobileNumber());
        userMap.put(FamilyConstants.JSON_KEY_GENDER, StringUtils.isNotBlank(profile.getGender()) ? profile.getGender() : FamilyConstants.GENDER_OTHERS);
        userMap.put(FamilyConstants.JSON_KEY_TYPE, FamilyConstants.USER_TYPE_CITIZEN);
        userMap.put(FamilyConstants.JSON_KEY_ACTIVE, true);

        requestBody.put(FamilyConstants.JSON_KEY_USER, userMap);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Explicitly ensure inter-service context by omitting or removing gateway pass-through header
        headers.remove("x-pass-through-gateway");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey(FamilyConstants.JSON_KEY_USER)) {
                List<Map<String, Object>> userList = (List<Map<String, Object>>) response.get(FamilyConstants.JSON_KEY_USER);
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
                    FamilyConstants.MSG_CALLER_INFO_REQUIRED_VERIFY);
        }

        String callerUuid = requestInfo.getUserInfo().getUuid().trim();
        // Allow internal system users to proceed
        if (FamilyConstants.SYSTEM_USER.equalsIgnoreCase(callerUuid)) {
            return null;
        }

        // Allow official employees to proceed
        String userType = requestInfo.getUserInfo().getType();
        if (FamilyConstants.USER_TYPE_EMPLOYEE.equalsIgnoreCase(userType)) {
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
                    FamilyConstants.MSG_USER_UUID_REQUIRED_VERIFY);
        }

        if (FamilyConstants.SYSTEM_USER.equalsIgnoreCase(userUuid)) {
            return null;
        }

        Map<String, CitizenProfile> profileMap = searchUsersByUuids(Collections.singletonList(userUuid), requestInfo);
        CitizenProfile profile = profileMap != null ? profileMap.get(userUuid) : null;
        if (profile == null) {
            log.error("[UPYOG-USER-CLIENT] User not found in database for UUID: {}", userUuid);
            throw new CustomException(FamilyConstants.ERROR_USER_NOT_FOUND,
                    FamilyConstants.MSG_CITIZEN_NOT_FOUND_REGISTRY_PREFIX + userUuid);
        }

        if (!Boolean.TRUE.equals(profile.getIsIdentityVerified())) {
            log.error("[UPYOG-USER-CLIENT] User identity not verified in database for UUID: {}", userUuid);
            throw new CustomException(FamilyConstants.ERROR_USER_NOT_VERIFIED,
                    FamilyConstants.MSG_USER_NOT_VERIFIED);
        }

        return profile;
    }

    /**
     * Synchronizes family member demographic profiles and asset declarations (properties, income,
     * vehicles, attached documents) to the core UPYOG registry in {@code egov-user}.
     *
     * @param members list of family members with asset declarations
     * @param tenantId tenant identifier
     * @param requestInfo UPYOG request context envelope
     * @return true if synchronization succeeded, false otherwise
     */
    @Override
    public boolean syncMemberAssets(List<FamilyMember> members, String tenantId, RequestInfo requestInfo) {
        if (members == null || members.isEmpty()) {
            return false;
        }

        if (configuration == null || StringUtils.isBlank(configuration.getUserHost())) {
            log.warn("[UPYOG-USER-CLIENT] egov.user.host is not configured; skipping member asset synchronization");
            return false;
        }

        String syncPath = StringUtils.isNotBlank(configuration.getUserMemberAssetSyncPath())
                ? configuration.getUserMemberAssetSyncPath()
                : FamilyConstants.DEFAULT_MEMBER_ASSET_SYNC_PATH;
        String url = buildUrl(configuration.getUserHost(), syncPath);

        log.info("[UPYOG-USER-CLIENT] Synchronizing assets for {} member(s) to egov-user at: {}", members.size(), url);

        List<Map<String, Object>> memberPayloadList = new ArrayList<>();
        for (FamilyMember m : members) {
            if (m == null || StringUtils.isBlank(m.getUserUuid())) {
                continue;
            }

            Map<String, Object> memberMap = new HashMap<>();
            memberMap.put(FamilyConstants.JSON_KEY_USER_UUID, m.getUserUuid());
            memberMap.put(FamilyConstants.PARAM_TENANT_ID, StringUtils.isNotBlank(m.getTenantId()) ? m.getTenantId() : tenantId);
            if (StringUtils.isNotBlank(m.getFamilyId())) {
                memberMap.put(FamilyConstants.JSON_KEY_FAMILY_ID, m.getFamilyId());
            }

            // Demographics & Documents from citizenDetails if present
            if (m.getCitizenDetails() != null) {
                CitizenProfile cp = m.getCitizenDetails();
                if (StringUtils.isNotBlank(cp.getCaste())) {
                    memberMap.put(FamilyConstants.JSON_KEY_CASTE, cp.getCaste());
                }
                if (cp.getIsSpeciallyAbled() != null) {
                    memberMap.put(FamilyConstants.JSON_KEY_IS_SPECIALLY_ABLED, cp.getIsSpeciallyAbled());
                }
                if (cp.getDocuments() != null && !cp.getDocuments().isEmpty()) {
                    memberMap.put(FamilyConstants.JSON_KEY_DOCUMENTS, cp.getDocuments());
                }
            }

            // Directly declared properties, income, vehicles on member
            if (m.getProperties() != null && !m.getProperties().isEmpty()) {
                memberMap.put(FamilyConstants.JSON_KEY_PROPERTIES, m.getProperties());
            }
            if (m.getIncome() != null) {
                memberMap.put(FamilyConstants.JSON_KEY_INCOME, m.getIncome());
            }
            if (m.getVehicles() != null && !m.getVehicles().isEmpty()) {
                memberMap.put(FamilyConstants.JSON_KEY_VEHICLES, m.getVehicles());
            }

            memberPayloadList.add(memberMap);
        }

        if (memberPayloadList.isEmpty()) {
            return false;
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(FamilyConstants.JSON_KEY_REQUEST_INFO, requestInfo);
        requestBody.put(FamilyConstants.JSON_KEY_MEMBERS, memberPayloadList);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Explicitly ensure inter-service context by omitting or removing gateway pass-through header
        headers.remove("x-pass-through-gateway");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<?, ?> response = restTemplate.postForObject(url, entity, Map.class);
            log.info("[UPYOG-USER-CLIENT] Successfully synchronized member assets with egov-user. Response: {}",
                    response != null ? response.get(FamilyConstants.JSON_KEY_STATUS) : FamilyConstants.STATUS_OK);
            return true;
        } catch (Exception e) {
            log.warn("[UPYOG-USER-CLIENT] Member asset synchronization to egov-user failed or unreachable: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Maps a raw user map returned by the user service into a strongly-typed CitizenProfile object.
     *
     * @param map raw user attributes map
     * @return populated CitizenProfile instance
     */
    private CitizenProfile mapToCitizenProfile(Map<String, Object> map) {
        Long dob = null;
        Object dobObj = map.get(FamilyConstants.JSON_KEY_DOB);
        if (dobObj instanceof Number) {
            dob = ((Number) dobObj).longValue();
        }

        Boolean isIdentityVerified = null;
        Object idVerifiedObj = map.get(FamilyConstants.JSON_KEY_IS_IDENTITY_VERIFIED);
        if (idVerifiedObj instanceof Boolean) {
            isIdentityVerified = (Boolean) idVerifiedObj;
        } else if (idVerifiedObj != null) {
            isIdentityVerified = Boolean.valueOf(String.valueOf(idVerifiedObj));
        }

        Boolean isSpeciallyAbled = null;
        Object speciallyAbledObj = map.get(FamilyConstants.JSON_KEY_IS_SPECIALLY_ABLED);
        if (speciallyAbledObj instanceof Boolean) {
            isSpeciallyAbled = (Boolean) speciallyAbledObj;
        } else if (speciallyAbledObj != null) {
            isSpeciallyAbled = Boolean.valueOf(String.valueOf(speciallyAbledObj));
        }

        String altMobile = (String) map.get(FamilyConstants.JSON_KEY_ALTERNATE_MOBILE_NUMBER);
        if (StringUtils.isBlank(altMobile)) {
            altMobile = (String) map.get("alternatemobilenumber");
        }

        String guardian = (String) map.get(FamilyConstants.JSON_KEY_GUARDIAN);
        if (StringUtils.isBlank(guardian)) {
            guardian = (String) map.get("fatherOrHusbandName");
        }

        String guardianRelation = (String) map.get(FamilyConstants.JSON_KEY_GUARDIAN_RELATION);
        if (StringUtils.isBlank(guardianRelation)) {
            guardianRelation = (String) map.get("relationship");
        }

        return CitizenProfile.builder()
                .uuid((String) map.get(FamilyConstants.JSON_KEY_UUID))
                .userName((String) map.get(FamilyConstants.JSON_KEY_USER_NAME))
                .name((String) map.get(FamilyConstants.JSON_KEY_NAME))
                .mobileNumber((String) map.get(FamilyConstants.JSON_KEY_MOBILE_NUMBER))
                .gender((String) map.get(FamilyConstants.JSON_KEY_GENDER))
                .dob(dob)
                .photo((String) map.get(FamilyConstants.JSON_KEY_PHOTO))
                .emailId((String) map.get(FamilyConstants.JSON_KEY_EMAIL_ID))
                .alternateMobileNumber(altMobile)
                .fatherName((String) map.get(FamilyConstants.JSON_KEY_FATHER_NAME))
                .motherName((String) map.get(FamilyConstants.JSON_KEY_MOTHER_NAME))
                .spouseName((String) map.get(FamilyConstants.JSON_KEY_SPOUSE_NAME))
                .guardian(guardian)
                .guardianRelation(guardianRelation)
                .caste((String) map.get(FamilyConstants.JSON_KEY_CASTE))
                .isSpeciallyAbled(isSpeciallyAbled)
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
