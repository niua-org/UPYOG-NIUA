package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.service.utils.UserConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST repository communicating with upyog-family-service to query active household memberships.
 *
 * <p>Enforces strict non-blocking fail-open resilience: if upyog-family-service is unreachable
 * or returns an error, it safely returns null without failing user profile or authentication operations.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class FamilyRestRepository {

    private final RestTemplate restTemplate;
    private final UserConfiguration userConfiguration;
    private final ObjectMapper objectMapper;

    @Autowired
    public FamilyRestRepository(@Autowired(required = false) RestTemplate restTemplate,
                                @Autowired(required = false) UserConfiguration userConfiguration,
                                @Autowired(required = false) ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.userConfiguration = userConfiguration;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Resolves the active Family ID for a citizen user UUID by querying upyog-family-service.
     *
     * @param userUuid    citizen unique identifier
     * @param tenantId    tenant identifier
     * @param requestInfo standard UPYOG RequestInfo context
     * @return active familyId string, or null if not found or on service error
     */
    public String getActiveFamilyIdByUserUuid(String userUuid, String tenantId, RequestInfo requestInfo) {
        if (StringUtils.isBlank(userUuid) || restTemplate == null) {
            return null;
        }

        String host = userConfiguration != null ? userConfiguration.getFamilyServiceHost() : null;
        if (StringUtils.isBlank(host)) {
            log.warn("[FAMILY-REST-REPO] upyog-family-service host configuration is missing. Skipping family ID lookup.");
            return null;
        }

        try {
            String endpoint = resolveEndpoint();
            UriComponentsBuilder uriBuilder = buildBaseUri(host, endpoint)
                    .queryParam(UserConstants.FAMILY_PARAM_MEMBER_USER_UUID, userUuid)
                    .queryParam(UserConstants.FAMILY_PARAM_STATUS, UserConstants.FAMILY_STATUS_ACTIVE);

            if (StringUtils.isNotBlank(tenantId)) {
                uriBuilder.queryParam(UserConstants.PARAM_TENANT_ID, tenantId);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RequestInfo> entity = new HttpEntity<>(requestInfo != null ? requestInfo : new RequestInfo(), headers);

            String url = uriBuilder.toUriString();
            log.debug("[FAMILY-REST-REPO] Querying upyog-family-service at: {}", url);
            String responseStr = restTemplate.postForObject(url, entity, String.class);

            JsonNode families = parseFamiliesNode(responseStr);
            String familyId = extractActiveFamilyId(families);
            if (StringUtils.isNotBlank(familyId)) {
                log.debug("[FAMILY-REST-REPO] Resolved active familyId: {} for user: {}", familyId, userUuid);
                return familyId;
            }
        } catch (Exception e) {
            log.warn("[FAMILY-REST-REPO] Non-blocking: Could not resolve familyId from upyog-family-service for user {}: {}",
                    userUuid, e.getMessage());
        }

        return null;
    }

    /**
     * Batch resolves active Family IDs for a list of citizen user UUIDs.
     *
     * @param userUuids   list of citizen UUIDs
     * @param tenantId    tenant identifier
     * @param requestInfo standard UPYOG RequestInfo context
     * @return map of userUuid -> active familyId
     */
    public Map<String, String> getActiveFamilyIdsByUserUuids(List<String> userUuids, String tenantId, RequestInfo requestInfo) {
        if (userUuids == null || userUuids.isEmpty() || restTemplate == null) {
            return Collections.emptyMap();
        }

        List<String> cleanUuids = userUuids.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();

        if (cleanUuids.isEmpty()) {
            return Collections.emptyMap();
        }

        String host = userConfiguration != null ? userConfiguration.getFamilyServiceHost() : null;
        if (StringUtils.isBlank(host)) {
            log.warn("[FAMILY-REST-REPO] upyog-family-service host configuration is missing. Skipping batch family resolution.");
            return Collections.emptyMap();
        }

        try {
            String endpoint = resolveEndpoint();
            UriComponentsBuilder uriBuilder = buildBaseUri(host, endpoint)
                    .queryParam(UserConstants.FAMILY_PARAM_MEMBER_USER_UUIDS, String.join(",", cleanUuids))
                    .queryParam(UserConstants.FAMILY_PARAM_STATUS, UserConstants.FAMILY_STATUS_ACTIVE);

            if (StringUtils.isNotBlank(tenantId)) {
                uriBuilder.queryParam(UserConstants.PARAM_TENANT_ID, tenantId);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RequestInfo> entity = new HttpEntity<>(requestInfo != null ? requestInfo : new RequestInfo(), headers);

            String url = uriBuilder.toUriString();
            log.debug("[FAMILY-REST-REPO] Batch querying upyog-family-service at: {}", url);
            String responseStr = restTemplate.postForObject(url, entity, String.class);

            return parseBatchFamilyMap(responseStr, cleanUuids);
        } catch (Exception e) {
            log.warn("[FAMILY-REST-REPO] Non-blocking: Could not batch resolve familyIds from upyog-family-service: {}",
                    e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String resolveEndpoint() {
        return (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getFamilyServiceSearchEndpoint()))
                ? userConfiguration.getFamilyServiceSearchEndpoint()
                : UserConstants.DEFAULT_FAMILY_SEARCH_ENDPOINT;
    }

    private UriComponentsBuilder buildBaseUri(String host, String endpoint) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(host);
        if (!host.endsWith("/") && !endpoint.startsWith("/")) {
            uriBuilder.path("/");
        }
        return uriBuilder.path(endpoint);
    }

    private JsonNode parseFamiliesNode(String responseStr) {
        if (StringUtils.isBlank(responseStr)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode families = root.path(UserConstants.JSON_KEY_FAMILIES_UPPER);
            if (!families.isArray() || families.isEmpty()) {
                families = root.path(UserConstants.JSON_KEY_FAMILIES_LOWER);
            }
            return families.isArray() ? families : null;
        } catch (Exception e) {
            log.debug("[FAMILY-REST-REPO] Error parsing family response JSON: {}", e.getMessage());
            return null;
        }
    }

    private String extractActiveFamilyId(JsonNode families) {
        if (families == null) {
            return null;
        }
        for (JsonNode familyNode : families) {
            String status = familyNode.path(UserConstants.FIELD_FAMILY_STATUS).asText(null);
            if (UserConstants.FAMILY_STATUS_ACTIVE.equalsIgnoreCase(status)) {
                String familyId = familyNode.path(UserConstants.FIELD_FAMILY_ID).asText(null);
                if (StringUtils.isNotBlank(familyId)) {
                    return familyId;
                }
            }
        }
        return null;
    }

    private Map<String, String> parseBatchFamilyMap(String responseStr, List<String> cleanUuids) {
        JsonNode families = parseFamiliesNode(responseStr);
        if (families == null) {
            return Collections.emptyMap();
        }

        Map<String, String> resultMap = new HashMap<>();
        for (JsonNode familyNode : families) {
            String status = familyNode.path(UserConstants.FIELD_FAMILY_STATUS).asText(null);
            if (UserConstants.FAMILY_STATUS_ACTIVE.equalsIgnoreCase(status)) {
                String familyId = familyNode.path(UserConstants.FIELD_FAMILY_ID).asText(null);
                if (StringUtils.isNotBlank(familyId)) {
                    populateMembersForFamily(familyNode, familyId, cleanUuids, resultMap);
                }
            }
        }
        return resultMap;
    }

    private void populateMembersForFamily(JsonNode familyNode, String familyId, List<String> cleanUuids, Map<String, String> resultMap) {
        JsonNode membersNode = familyNode.path(UserConstants.JSON_KEY_MEMBERS);
        if (!membersNode.isArray()) {
            return;
        }
        for (JsonNode memberNode : membersNode) {
            boolean isActiveMember = memberNode.path(UserConstants.JSON_KEY_ACTIVE).asBoolean(true);
            String memberUuid = memberNode.path(UserConstants.JSON_KEY_USER_UUID).asText(null);
            if (isActiveMember && StringUtils.isNotBlank(memberUuid) && cleanUuids.contains(memberUuid)) {
                resultMap.putIfAbsent(memberUuid, familyId);
            }
        }
    }
}
