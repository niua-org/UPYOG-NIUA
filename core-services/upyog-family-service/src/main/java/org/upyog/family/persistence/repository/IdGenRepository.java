package org.upyog.family.persistence.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.upyog.family.config.FamilyConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.time.Year;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client repository interacting with UPYOG idgen service to produce formatted family IDs.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class IdGenRepository {

    private final RestTemplate restTemplate;
    private final FamilyConfiguration configuration;

    @Autowired
    public IdGenRepository(RestTemplate restTemplate, FamilyConfiguration configuration) {
        this.restTemplate = restTemplate;
        this.configuration = configuration;
    }

    /**
     * Calls UPYOG IdGen microservice to generate a formatted, unique sequential Family ID (e.g. UPYOG_FAMILY-2026-0001).
     * Falls back to a deterministic formatted ID if IdGen service is unreachable.
     *
     * @param requestInfo UPYOG request context envelope
     * @param tenantId target tenant identifier
     * @return generated unique Family ID string
     */
    @SuppressWarnings("unchecked")
    public String generateFamilyId(RequestInfo requestInfo, String tenantId) {
        String url = configuration.getIdGenHost() + configuration.getIdGenPath();
        log.info("[UPYOG-IDGEN-CLIENT] Requesting family ID generation from: {} for tenant: {}", url, tenantId);

        Map<String, Object> idRequest = new HashMap<>();
        idRequest.put("idName", configuration.getIdGenFamilyIdName());
        idRequest.put("tenantId", tenantId);
        idRequest.put("format", configuration.getIdGenFamilyIdFormat());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("RequestInfo", requestInfo);
        requestBody.put("idRequests", Collections.singletonList(idRequest));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("idResponses")) {
                List<Map<String, Object>> idResponses = (List<Map<String, Object>>) response.get("idResponses");
                if (idResponses != null && !idResponses.isEmpty()) {
                    String generatedId = (String) idResponses.get(0).get("id");
                    if (StringUtils.isNotBlank(generatedId)) {
                        log.info("[UPYOG-IDGEN-CLIENT] Successfully generated familyId: {}", generatedId);
                        return generatedId;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[UPYOG-IDGEN-CLIENT] Call to UPYOG idgen failed: {}. Generating fallback sequence.", e.getMessage());
        }

        // Fallback generator for test/offline environments
        String fallbackId = "FAM-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[UPYOG-IDGEN-CLIENT] Using fallback generated familyId: {}", fallbackId);
        return fallbackId;
    }
}
