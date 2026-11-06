package org.egov.dx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.dx.service.UserRegistryService;
import org.egov.dx.util.AadhaarConstants;
import org.egov.dx.util.AadhaarNumberUtils;
import org.egov.dx.util.Configurations;
import org.egov.dx.web.models.UserDemographics;
import org.egov.dx.web.models.UserRegistryRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

    @Value("${egov.user.aadhaar.search.path}")
    private String aadhaarSearchPath;

    @Value("${egov.user.profile.search.path}")
    private String profileSearchPath;

    @Value("${egov.user.search.path}")
    private String userSearchPath;

    @Value("${egov.user.create.novalidate.path}")
    private String userCreatePath;

    @Value("${egov.user.aadhaar.mark.verified.path}")
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
        UserRegistryRecord record = searchCitizenByAadhaar(aadhaarNumber, tenantId, requestInfo);
        return record != null ? record.getUserUuid() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserRegistryRecord searchCitizenByAadhaar(String aadhaarNumber, String tenantId, RequestInfo requestInfo) {
        String fullAadhaar = AadhaarNumberUtils.normalizeToTwelveDigits(aadhaarNumber);
        if (fullAadhaar == null) {
            return null;
        }

        if (StringUtils.isBlank(aadhaarSearchPath)) {
            throw new CustomException("INVALID_CONFIGURATION", "egov.user.aadhaar.search.path is not configured");
        }

        String effectiveTenantId = resolveTenantId(tenantId);
        String userHost = resolveUserHost();

        String searchUrl = buildUrl(userHost, aadhaarSearchPath);
        return searchCitizenRecordByFullAadhaar(searchUrl, fullAadhaar, effectiveTenantId, requestInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String searchFamilyIdByAadhaar(String aadhaarNumber, String tenantId, RequestInfo requestInfo) {
        UserRegistryRecord record = searchCitizenByAadhaar(aadhaarNumber, tenantId, requestInfo);
        return record != null ? record.getFamilyId() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String searchFamilyIdByUserUuid(String userUuid, String tenantId, RequestInfo requestInfo) {
        if (StringUtils.isBlank(userUuid)) {
            return null;
        }

        // 1. Try querying upyog-family-service directly if configured
        String familyId = queryFamilyServiceForFamilyId(userUuid, tenantId, requestInfo);
        if (StringUtils.isNotBlank(familyId)) {
            return familyId;
        }

        // 2. Fallback to querying egov-user profile search endpoint
        if (StringUtils.isNotBlank(profileSearchPath)) {
            try {
                String url = buildUrl(resolveUserHost(), profileSearchPath);
                Map<String, Object> reqBody = new HashMap<>();
                reqBody.put("RequestInfo", requestInfo != null ? requestInfo : new RequestInfo());
                reqBody.put("tenantId", resolveTenantId(tenantId));
                reqBody.put("uuid", userUuid);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

                String responseStr = restTemplate.postForObject(url, entity, String.class);
                if (StringUtils.isNotBlank(responseStr)) {
                    JsonNode root = objectMapper.readTree(responseStr);
                    JsonNode userArray = root.path("user");
                    if (!userArray.isArray() || userArray.isEmpty()) {
                        userArray = root.path("User");
                    }
                    if (userArray.isArray() && !userArray.isEmpty()) {
                        return extractFamilyIdNode(userArray.get(0));
                    }
                }
            } catch (Exception e) {
                log.warn("[USER-REGISTRY] Non-blocking: Failed to query familyId from profile search for user {}: {}",
                        userUuid, e.getMessage());
            }
        }
        return null;
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
            throw new CustomException("INVALID_USER_UUID", "User UUID is mandatory to mark citizen as verified");
        }
        if (StringUtils.isBlank(aadhaarNumber)) {
            throw new CustomException(AadhaarConstants.ERR_CODE_INVALID_AADHAAR, "Aadhaar number is mandatory");
        }
        String fullAadhaar = AadhaarNumberUtils.normalizeToTwelveDigits(aadhaarNumber);
        if (fullAadhaar == null) {
            log.warn("[USER-REGISTRY] Cannot mark citizen verified: Aadhaar is not a valid 12-digit value");
            throw new CustomException(AadhaarConstants.ERR_CODE_INVALID_AADHAAR, AadhaarConstants.MSG_INVALID_AADHAAR);
        }

        if (StringUtils.isBlank(markVerifiedPath)) {
            throw new CustomException("INVALID_CONFIGURATION", "egov.user.aadhaar.mark.verified.path is not configured");
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
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[USER-REGISTRY] Failed to mark citizen {} as Aadhaar-verified: {}", userUuid, e.getMessage());
        }
    }

    private UserRegistryRecord searchCitizenRecordByFullAadhaar(String url, String fullAadhaar, String tenantId, RequestInfo requestInfo) {
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

            // Intentionally no fallback to first/unverified user: UUID is returned only on full 12-digit Aadhaar match
            if (userArray.isArray() && !userArray.isEmpty()) {
                for (JsonNode userData : userArray) {
                    boolean active = !userData.has("active") || userData.path("active").asBoolean(true);
                    if (!active) {
                        continue;
                    }
                    String storedAadhaar = userData.path("aadhaarNumber").asText(null);
                    String normalizedStored = AadhaarNumberUtils.normalizeToTwelveDigits(storedAadhaar);
                    if (normalizedStored != null && fullAadhaar.equals(normalizedStored)) {
                        String candidateUuid = userData.path("uuid").asText(null);
                        if (StringUtils.isNotBlank(candidateUuid)) {
                            String candidateFamilyId = extractFamilyIdNode(userData);
                            log.debug("[USER-REGISTRY] Resolved citizen UUID: {} with familyId: {} from full Aadhaar match",
                                    candidateUuid, candidateFamilyId);
                            log.info("[USER-REGISTRY] Resolved citizen from full Aadhaar match; familyId present={}",
                                    StringUtils.isNotBlank(candidateFamilyId));
                            return UserRegistryRecord.builder()
                                    .userUuid(candidateUuid)
                                    .familyId(candidateFamilyId)
                                    .build();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[USER-REGISTRY] Failed to query citizen by Aadhaar from URL [{}]: {}", url, e.getMessage());
        }
        return null;
    }

    /**
     * Extracts and sanitizes the {@code familyId} field from a user JSON node.
     * Treats missing nodes, blank values, and literal string "null" as absent.
     *
     * @param userData JSON node containing user attributes
     * @return trimmed family identifier, or {@code null} if absent or blank
     */
    private String extractFamilyIdNode(JsonNode userData) {
        if (userData == null || !userData.hasNonNull("familyId")) {
            return null;
        }
        String familyId = userData.path("familyId").asText(null);
        if (StringUtils.isNotBlank(familyId) && !"null".equalsIgnoreCase(familyId.trim())) {
            return familyId.trim();
        }
        return null;
    }

    /**
     * Queries upyog-family-service directly for the active Family ID of a citizen member.
     *
     * <p>Applies an {@code ACTIVE} status filter and normalizes tenant to state level.
     * If {@code family.service.host} is unconfigured or blank, the call is skipped returning {@code null}.
     * Fails open in a non-blocking manner on network or service errors so caller flows are not disrupted.</p>
     *
     * @param userUuid    unique citizen member UUID
     * @param tenantId    caller tenant identifier
     * @param requestInfo standard RequestInfo context
     * @return active family identifier, or {@code null} if not found or skipped
     */
    private String queryFamilyServiceForFamilyId(String userUuid, String tenantId, RequestInfo requestInfo) {
        String familyHost = configurations.getFamilyServiceHost();
        if (StringUtils.isBlank(familyHost)) {
            return null;
        }
        try {
            String endpoint = StringUtils.defaultIfBlank(configurations.getFamilyServiceSearchEndpoint(), "family/v1/family/_search");
            if (!familyHost.endsWith("/")) {
                familyHost += "/";
            }
            if (endpoint.startsWith("/")) {
                endpoint = endpoint.substring(1);
            }
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(familyHost + endpoint)
                    .queryParam("memberUserUuid", userUuid)
                    .queryParam("status", "ACTIVE");
            if (StringUtils.isNotBlank(tenantId)) {
                uriBuilder.queryParam("tenantId", resolveTenantId(tenantId));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RequestInfo> entity = new HttpEntity<>(requestInfo != null ? requestInfo : new RequestInfo(), headers);

            String responseStr = restTemplate.postForObject(uriBuilder.toUriString(), entity, String.class);
            if (StringUtils.isNotBlank(responseStr)) {
                JsonNode root = objectMapper.readTree(responseStr);
                JsonNode families = root.path("families");
                if (!families.isArray() || families.isEmpty()) {
                    families = root.path("Families");
                }
                if (families.isArray() && !families.isEmpty()) {
                    for (JsonNode family : families) {
                        String fid = family.path("familyId").asText(null);
                        if (StringUtils.isNotBlank(fid) && !"null".equalsIgnoreCase(fid.trim())) {
                            return fid.trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[USER-REGISTRY] Non-blocking: Could not resolve familyId from family service: {}", e.getMessage());
        }
        return null;
    }

    private String autoCreateCitizen(String aadhaarNumber, UserDemographics demographics, String tenantId, RequestInfo requestInfo) {
        try {
            if (StringUtils.isBlank(userCreatePath)) {
                throw new CustomException("INVALID_CONFIGURATION", "egov.user.create.novalidate.path is not configured");
            }
            if (StringUtils.isBlank(aadhaarNumber)) {
                throw new CustomException(AadhaarConstants.ERR_CODE_INVALID_AADHAAR, "Aadhaar number is mandatory");
            }
            String fullAadhaar = AadhaarNumberUtils.normalizeToTwelveDigits(aadhaarNumber);
            if (fullAadhaar == null) {
                throw new CustomException(AadhaarConstants.ERR_CODE_INVALID_AADHAAR, AadhaarConstants.MSG_INVALID_AADHAAR);
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
                String createdUuid = userArray.get(0).path("uuid").asText(null);
                if (StringUtils.isNotBlank(createdUuid)) {
                    log.info("[USER-REGISTRY] Successfully auto-provisioned citizen with UUID: {}", createdUuid);
                    return createdUuid;
                }
            }
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[USER-REGISTRY] Failed to auto-provision citizen in egov-user: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Resolves egov-user host from configuration; fails fast when not configured.
     *
     * @return normalized host URL ending with {@code /}
     */
    private String resolveUserHost() {
        String host = configurations.getUserHost();
        if (StringUtils.isBlank(host)) {
            throw new CustomException(AadhaarConstants.ERR_CODE_INVALID_REQUEST,
                    "egov.user.host is not configured for user registry integration");
        }
        if (!host.endsWith("/")) {
            host += "/";
        }
        return host;
    }

    /**
     * Builds an absolute URL from host and configured API path segment.
     *
     * @param host service host
     * @param path configured path (must not be blank)
     * @return absolute URL
     */
    private String buildUrl(String host, String path) {
        if (StringUtils.isBlank(path)) {
            throw new CustomException(AadhaarConstants.ERR_CODE_INVALID_REQUEST,
                    "egov-user API path is not configured for user registry integration");
        }
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        return host + cleanPath;
    }

    /**
     * Normalizes tenant to state-level code; tenant is mandatory for registry calls.
     *
     * @param tenantId caller tenant identifier
     * @return state-level tenant id
     */
    private String resolveTenantId(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            throw new CustomException(AadhaarConstants.ERR_CODE_INVALID_REQUEST, "tenantId is mandatory");
        }
        return tenantId.contains(".") ? tenantId.split("\\.")[0] : tenantId;
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
