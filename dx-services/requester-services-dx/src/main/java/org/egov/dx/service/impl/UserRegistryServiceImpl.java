package org.egov.dx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.service.UserRegistryService;
import org.egov.dx.util.AadhaarConstants;
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

/**
 * Implementation of {@link UserRegistryService} querying egov-user service endpoints
 * to locate existing citizen accounts matching a verified Aadhaar number.
 *
 * <p>Adheres strictly to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles the lookup and extraction of citizen UUIDs from egov-user.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Pure constructor injection without field autowiring.</li>
 * </ul>
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

    @Autowired
    public UserRegistryServiceImpl(RestTemplate restTemplate,
                                  Configurations configurations,
                                  ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.configurations = configurations;
        this.objectMapper = objectMapper;
    }

    @Override
    public String searchUserUuidByAadhaar(String aadhaarNumber, String tenantId, RequestInfo requestInfo) {
        if (StringUtils.isBlank(aadhaarNumber)) {
            return null;
        }

        String effectiveTenantId = resolveTenantId(tenantId);
        String userHost = resolveUserHost();

        // 1. Query standard user search endpoint (/user/_search)
        String standardSearchUrl = buildUrl(userHost, userSearchPath);
        String uuid = executeSearchFlow(standardSearchUrl, aadhaarNumber, effectiveTenantId, requestInfo);
        if (StringUtils.isNotBlank(uuid)) {
            return uuid;
        }

        // 2. Fallback to profile search endpoint if configured differently
        String profileSearchUrl = buildUrl(userHost, profileSearchPath);
        if (!profileSearchUrl.equalsIgnoreCase(standardSearchUrl)) {
            uuid = executeSearchFlow(profileSearchUrl, aadhaarNumber, effectiveTenantId, requestInfo);
            if (StringUtils.isNotBlank(uuid)) {
                return uuid;
            }
        }

        return null;
    }

    @Override
    public String resolveOrCreateUserUuid(String aadhaarNumber, UserDemographics demographics, String tenantId, RequestInfo requestInfo) {
        String existingUuid = searchUserUuidByAadhaar(aadhaarNumber, tenantId, requestInfo);
        if (StringUtils.isNotBlank(existingUuid)) {
            log.info("[USER-REGISTRY] Resolved existing citizen UUID: {} for Aadhaar", existingUuid);
            return existingUuid;
        }

        log.info("[USER-REGISTRY] Citizen not found for Aadhaar. Auto-provisioning new citizen profile in egov-user.");
        return autoCreateCitizen(aadhaarNumber, demographics, tenantId, requestInfo);
    }

    private String executeSearchFlow(String url, String aadhaarNumber, String tenantId, RequestInfo requestInfo) {
        // 1. If aadhaarNumber is raw 12 digits, try masked version first ("XXXXXXXX" + last 4 digits) as registered citizens use AADHAAR_<last4>
        if (aadhaarNumber != null && aadhaarNumber.matches("^[0-9]{12}$")) {
            String maskedAadhaar = "XXXXXXXX" + aadhaarNumber.substring(8);
            String uuid = executeSearch(url, maskedAadhaar, tenantId, requestInfo);
            if (StringUtils.isNotBlank(uuid)) {
                return uuid;
            }
        }

        // 2. Try search with the provided Aadhaar identifier (raw or masked)
        String uuid = executeSearch(url, aadhaarNumber, tenantId, requestInfo);
        if (StringUtils.isNotBlank(uuid)) {
            return uuid;
        }

        return null;
    }

    private String executeSearch(String url, String aadhaar, String tenantId, RequestInfo requestInfo) {
        try {
            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("RequestInfo", requestInfo != null ? requestInfo : new RequestInfo());
            reqBody.put("tenantId", tenantId);

            String digits = aadhaar != null ? aadhaar.replaceAll("\\D", "") : "";
            if (StringUtils.isNotBlank(digits)) {
                reqBody.put("userName", AadhaarConstants.AADHAAR_USERNAME_PREFIX + digits);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            log.debug("[USER-REGISTRY] Executing search query to URL: {} for tenant: {}", url, tenantId);
            String responseStr = restTemplate.postForObject(url, entity, String.class);
            if (StringUtils.isBlank(responseStr)) {
                return null;
            }

            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode userArray = root.path("user");
            if (!userArray.isArray() || userArray.isEmpty()) {
                userArray = root.path("User");
            }

            if (userArray.isArray() && !userArray.isEmpty()) {
                String fallbackUuid = null;
                for (JsonNode u : userArray) {
                    boolean active = !u.has("active") || u.path("active").asBoolean(true);
                    if (active) {
                        boolean identityVerified = u.path("isIdentityVerified").asBoolean(false);
                        String candidateUuid = u.path("uuid").asText(null);
                        if (StringUtils.isNotBlank(candidateUuid)) {
                            if (identityVerified) {
                                log.info("[USER-REGISTRY] Resolved verified citizen UUID: {} from egov-user", candidateUuid);
                                return candidateUuid;
                            }
                            if (fallbackUuid == null) {
                                fallbackUuid = candidateUuid;
                            }
                        }
                    }
                }
                if (fallbackUuid != null) {
                    log.info("[USER-REGISTRY] Resolved active citizen UUID: {} from egov-user", fallbackUuid);
                    return fallbackUuid;
                }
                String firstUuid = userArray.get(0).path("uuid").asText(null);
                if (StringUtils.isNotBlank(firstUuid)) {
                    log.info("[USER-REGISTRY] Resolved citizen UUID: {} from egov-user", firstUuid);
                    return firstUuid;
                }
            }
        } catch (Exception e) {
            log.warn("[USER-REGISTRY] Failed to query citizen by Aadhaar from URL [{}]: {}", url, e.getMessage());
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
            return tenantId;
        }
        return "pg";
    }

    private String autoCreateCitizen(String aadhaarNumber, UserDemographics demographics, String tenantId, RequestInfo requestInfo) {
        try {
            String effectiveTenantId = resolveTenantId(tenantId);
            String userHost = resolveUserHost();
            String createUrl = buildUrl(userHost, userCreatePath);

            String maskedAadhaar = formatMaskedAadhaar(aadhaarNumber);

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

            if (StringUtils.isNotBlank(maskedAadhaar)) {
                userMap.put("aadhaarNumber", maskedAadhaar);
                userMap.put("userName", AadhaarConstants.AADHAAR_USERNAME_PREFIX + maskedAadhaar.replaceAll("\\D", ""));
            }
            userMap.put("active", true);
            userMap.put("type", AadhaarConstants.USER_TYPE_CITIZEN);
            userMap.put("tenantId", effectiveTenantId);
            userMap.put("isIdentityVerified", true);
            userMap.put("identityVerifiedSource", AadhaarConstants.IDENTITY_SOURCE_AADHAAR);

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
                log.warn("[USER-REGISTRY] Empty response received from citizen auto-creation at {}", createUrl);
                return null;
            }

            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode userArray = root.path("user");
            if (!userArray.isArray() || userArray.isEmpty()) {
                userArray = root.path("User");
            }

            if (userArray.isArray() && !userArray.isEmpty()) {
                String createdUuid = userArray.get(0).path("uuid").asText(null);
                if (StringUtils.isNotBlank(createdUuid)) {
                    log.info("[USER-REGISTRY] Successfully auto-provisioned citizen with UUID: {}", createdUuid);
                    return createdUuid;
                }
            }
        } catch (Exception e) {
            log.error("[USER-REGISTRY] Failed to auto-provision citizen in egov-user: {}", e.getMessage(), e);
        }
        return null;
    }

    private String formatMaskedAadhaar(String aadhaarNumber) {
        if (StringUtils.isBlank(aadhaarNumber)) {
            return null;
        }
        String digits = aadhaarNumber.replaceAll("\\D", "");
        if (digits.length() == 12) {
            return "XXXXXXXX" + digits.substring(8);
        }
        if (aadhaarNumber.toUpperCase().startsWith("XXXXXXXX") && aadhaarNumber.length() == 12) {
            return aadhaarNumber.toUpperCase();
        }
        if (digits.length() >= 4) {
            return "XXXXXXXX" + digits.substring(digits.length() - 4);
        }
        return aadhaarNumber;
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
