package org.egov.dx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.service.UserRegistryService;
import org.egov.dx.util.AadhaarConstants;
import org.egov.dx.util.AadhaarNumberUtils;
import org.egov.dx.util.Configurations;
import org.egov.dx.web.models.UserDemographics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves and provisions egov-user citizen records using full 12-digit Aadhaar matching after verified e-KYC.
 *
 * <p>TODO: mask Aadhaar and move plain-text storage to Aadhaar Data Vault when integrating with UIDAI.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class UserRegistryServiceImpl implements UserRegistryService {

    private final RestTemplate restTemplate;
    private final Configurations configurations;
    private final ObjectMapper objectMapper;

    @Value("${egov.user.profile.search.path:/user/profile/_search}")
    private String profileSearchPath;

    @Value("${egov.user.search.path:/user/_search}")
    private String userSearchPath;

    @Value("${egov.user.create.novalidate.path:/user/users/_createnovalidate}")
    private String userCreatePath;

    @Value("${egov.user.aadhaar.mark.verified.path:/user/profile/aadhaar/_markVerifiedAfterEkyc}")
    private String markVerifiedPath;

    @Autowired
    public UserRegistryServiceImpl(RestTemplate restTemplate,
                                  Configurations configurations,
                                  ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.configurations = configurations;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String searchUserUuidByAadhaar(String aadhaarNumber, String tenantId, RequestInfo requestInfo) {
        String fullAadhaar = AadhaarNumberUtils.normalizeToTwelveDigits(aadhaarNumber);
        if (fullAadhaar == null) {
            return null;
        }

        String effectiveTenantId = resolveTenantId(tenantId);
        String userHost = resolveUserHost();

        String standardSearchUrl = buildUrl(userHost, userSearchPath);
        String uuid = searchByFullAadhaar(standardSearchUrl, fullAadhaar, effectiveTenantId, requestInfo);
        if (StringUtils.isNotBlank(uuid)) {
            return uuid;
        }

        String profileSearchUrl = buildUrl(userHost, profileSearchPath);
        if (!profileSearchUrl.equalsIgnoreCase(standardSearchUrl)) {
            uuid = searchByFullAadhaar(profileSearchUrl, fullAadhaar, effectiveTenantId, requestInfo);
        }
        return uuid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolveOrCreateUserUuid(String aadhaarNumber, UserDemographics demographics, String tenantId, RequestInfo requestInfo) {
        String existingUuid = searchUserUuidByAadhaar(aadhaarNumber, tenantId, requestInfo);
        if (StringUtils.isNotBlank(existingUuid)) {
            log.info("[USER-REGISTRY] Resolved existing citizen UUID for verified Aadhaar lookup");
            return existingUuid;
        }

        log.info("[USER-REGISTRY] Citizen not found for Aadhaar. Auto-provisioning new citizen profile in egov-user.");
        return autoCreateCitizen(aadhaarNumber, demographics, tenantId, requestInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markCitizenAadhaarVerifiedAfterEkyc(String userUuid, String aadhaarNumber, String tenantId, RequestInfo requestInfo) {
        if (StringUtils.isBlank(userUuid)) {
            return;
        }
        String fullAadhaar = AadhaarNumberUtils.normalizeToTwelveDigits(aadhaarNumber);
        if (fullAadhaar == null) {
            log.warn("[USER-REGISTRY] Cannot mark citizen verified: Aadhaar is not a valid 12-digit value");
            return;
        }

        try {
            String url = buildUrl(resolveUserHost(), markVerifiedPath);
            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("RequestInfo", requestInfo != null ? requestInfo : new RequestInfo());
            reqBody.put("userUuid", userUuid);
            reqBody.put("aadhaarNumber", fullAadhaar);
            reqBody.put("tenantId", resolveTenantId(tenantId));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            log.info("[USER-REGISTRY] Marking citizen UUID {} as Aadhaar-verified via egov-user profile service", userUuid);
            restTemplate.postForObject(url, entity, String.class);
        } catch (Exception e) {
            log.error("[USER-REGISTRY] Failed to mark citizen {} as Aadhaar-verified: {}", userUuid, e.getMessage());
        }
    }

    private String searchByFullAadhaar(String url, String fullAadhaar, String tenantId, RequestInfo requestInfo) {
        try {
            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("RequestInfo", requestInfo != null ? requestInfo : new RequestInfo());
            reqBody.put("tenantId", tenantId);
            // TODO: mask Aadhaar and move plain-text storage to Aadhaar Data Vault when integrating with UIDAI
            reqBody.put("aadhaarNumber", fullAadhaar);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            String responseStr = restTemplate.postForObject(url, entity, String.class);
            if (StringUtils.isBlank(responseStr)) {
                return null;
            }

            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode userArray = root.path("user");
            if (!userArray.isArray() || userArray.isEmpty()) {
                userArray = root.path("User");
            }

            if (userArray.isArray()) {
                for (JsonNode u : userArray) {
                    boolean active = !u.has("active") || u.path("active").asBoolean(true);
                    if (!active) {
                        continue;
                    }
                    String storedAadhaar = u.path("aadhaarNumber").asText(null);
                    String normalizedStored = AadhaarNumberUtils.normalizeToTwelveDigits(storedAadhaar);
                    if (normalizedStored != null && fullAadhaar.equals(normalizedStored)) {
                        String candidateUuid = u.path("uuid").asText(null);
                        if (StringUtils.isNotBlank(candidateUuid)) {
                            log.info("[USER-REGISTRY] Resolved citizen UUID from full Aadhaar match");
                            return candidateUuid;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[USER-REGISTRY] Failed to query citizen by Aadhaar from URL [{}]: {}", url, e.getMessage());
        }
        return null;
    }

    private String autoCreateCitizen(String aadhaarNumber, UserDemographics demographics, String tenantId, RequestInfo requestInfo) {
        try {
            String fullAadhaar = AadhaarNumberUtils.normalizeToTwelveDigits(aadhaarNumber);
            if (fullAadhaar == null) {
                log.warn("[USER-REGISTRY] Auto-provision skipped: invalid Aadhaar number");
                return null;
            }

            String effectiveTenantId = resolveTenantId(tenantId);
            String createUrl = buildUrl(resolveUserHost(), userCreatePath);

            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("RequestInfo", requestInfo != null ? requestInfo : new RequestInfo());

            Map<String, Object> userMap = new HashMap<>();
            String name = (demographics != null && StringUtils.isNotBlank(demographics.getName()))
                    ? demographics.getName().trim()
                    : "Citizen";
            userMap.put("name", name);

            if (demographics != null && StringUtils.isNotBlank(demographics.getGender())) {
                userMap.put("gender", normalizeGender(demographics.getGender()));
            }
            if (demographics != null && StringUtils.isNotBlank(demographics.getDob())) {
                userMap.put("dob", demographics.getDob());
            }
            if (demographics != null && StringUtils.isNotBlank(demographics.getPhoto())) {
                userMap.put("photo", demographics.getPhoto());
            }
            if (demographics != null && StringUtils.isNotBlank(demographics.getFullAddress())) {
                userMap.put("permanentAddress", demographics.getFullAddress());
                userMap.put("correspondenceAddress", demographics.getFullAddress());
            }
            if (demographics != null && StringUtils.isNotBlank(demographics.getCity()) && !"N/A".equalsIgnoreCase(demographics.getCity())) {
                userMap.put("permanentCity", demographics.getCity());
                userMap.put("correspondenceCity", demographics.getCity());
            }
            if (demographics != null && StringUtils.isNotBlank(demographics.getPincode()) && !"N/A".equalsIgnoreCase(demographics.getPincode())) {
                userMap.put("permanentPinCode", demographics.getPincode());
                userMap.put("correspondencePinCode", demographics.getPincode());
            }

            // TODO: mask Aadhaar and move plain-text storage to Aadhaar Data Vault when integrating with UIDAI
            userMap.put("aadhaarNumber", fullAadhaar);
            userMap.put("userName", AadhaarConstants.CITIZEN_USERNAME_PREFIX + UUID.randomUUID().toString().substring(0, 8));
            userMap.put("active", true);
            userMap.put("type", AadhaarConstants.USER_TYPE_CITIZEN);
            userMap.put("tenantId", effectiveTenantId);

            Map<String, Object> role = new HashMap<>();
            role.put("code", AadhaarConstants.ROLE_CITIZEN);
            role.put("name", AadhaarConstants.ROLE_CITIZEN_NAME);
            role.put("tenantId", effectiveTenantId);
            userMap.put("roles", Collections.singletonList(role));

            reqBody.put("user", userMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            log.info("[USER-REGISTRY] Auto-provisioning citizen at URL: {} for tenant: {}", createUrl, effectiveTenantId);
            String responseStr = restTemplate.postForObject(createUrl, entity, String.class);
            if (StringUtils.isBlank(responseStr)) {
                return null;
            }

            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode userArray = root.path("user");
            if (!userArray.isArray() || userArray.isEmpty()) {
                userArray = root.path("User");
            }

            if (userArray.isArray() && !userArray.isEmpty()) {
                return userArray.get(0).path("uuid").asText(null);
            }
        } catch (Exception e) {
            log.error("[USER-REGISTRY] Failed to auto-provision citizen in egov-user: {}", e.getMessage(), e);
        }
        return null;
    }

    private String resolveUserHost() {
        String host = configurations.getUserHost();
        if (StringUtils.isBlank(host)) {
            host = "http://localhost:8085/";
        }
        if (!host.endsWith("/")) {
            host += "/";
        }
        return host;
    }

    private String buildUrl(String host, String path) {
        String cleanPath = StringUtils.isNotBlank(path) ? path : "/user/profile/_search";
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        return host + cleanPath;
    }

    private String resolveTenantId(String tenantId) {
        if (StringUtils.isNotBlank(tenantId)) {
            return tenantId.contains(".") ? tenantId.split("\\.")[0] : tenantId;
        }
        return "pg";
    }

    private String normalizeGender(String gender) {
        if (StringUtils.isBlank(gender)) {
            return "OTHERS";
        }
        String upper = gender.trim().toUpperCase();
        if (upper.startsWith("M")) {
            return "MALE";
        } else if (upper.startsWith("F")) {
            return "FEMALE";
        } else if (upper.startsWith("T")) {
            return "TRANSGENDER";
        }
        return "OTHERS";
    }
}
