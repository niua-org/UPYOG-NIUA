package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.config.UserConfiguration;
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

        try {
            String host = (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getFamilyServiceHost()))
                    ? userConfiguration.getFamilyServiceHost()
                    : "http://localhost:8081/";
            String endpoint = (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getFamilyServiceSearchEndpoint()))
                    ? userConfiguration.getFamilyServiceSearchEndpoint()
                    : "family/v1/family/_search";

            StringBuilder urlBuilder = new StringBuilder(host);
            if (!host.endsWith("/") && !endpoint.startsWith("/")) {
                urlBuilder.append("/");
            } else if (host.endsWith("/") && endpoint.startsWith("/")) {
                endpoint = endpoint.substring(1);
            }
            urlBuilder.append(endpoint);
            urlBuilder.append("?memberUserUuid=").append(userUuid);
            if (StringUtils.isNotBlank(tenantId)) {
                urlBuilder.append("&tenantId=").append(tenantId);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RequestInfo> entity = new HttpEntity<>(requestInfo != null ? requestInfo : new RequestInfo(), headers);

            log.debug("[FAMILY-REST-REPO] Querying upyog-family-service at: {}", urlBuilder);
            String responseStr = restTemplate.postForObject(urlBuilder.toString(), entity, String.class);

            if (StringUtils.isNotBlank(responseStr)) {
                JsonNode root = objectMapper.readTree(responseStr);
                JsonNode families = root.path("Families");
                if (!families.isArray() || families.size() == 0) {
                    families = root.path("families");
                }
                if (families.isArray() && families.size() > 0) {
                    JsonNode firstFamily = families.get(0);
                    String familyId = firstFamily.path("familyId").asText(null);
                    if (StringUtils.isNotBlank(familyId)) {
                        log.debug("[FAMILY-REST-REPO] Resolved familyId: {} for user: {}", familyId, userUuid);
                        return familyId;
                    }
                }
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
        if (userUuids == null || userUuids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> resultMap = new HashMap<>();
        for (String uuid : userUuids) {
            if (StringUtils.isNotBlank(uuid)) {
                String familyId = getActiveFamilyIdByUserUuid(uuid, tenantId, requestInfo);
                if (StringUtils.isNotBlank(familyId)) {
                    resultMap.put(uuid, familyId);
                }
            }
        }
        return resultMap;
    }
}
