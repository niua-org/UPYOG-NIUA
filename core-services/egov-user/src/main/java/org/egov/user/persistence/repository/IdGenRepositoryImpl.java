package org.egov.user.persistence.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link IdGenRepository} interacting with UPYOG idgen service.
 * Supports configurable format resolution via UPYOG MDMS (module 'common-masters', master 'IdFormat').
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class IdGenRepositoryImpl implements IdGenRepository {

    private final RestTemplate restTemplate;
    private final UserConfiguration configuration;

    @Autowired
    public IdGenRepositoryImpl(RestTemplate restTemplate, UserConfiguration configuration) {
        this.restTemplate = restTemplate;
        this.configuration = configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateCitizenId(RequestInfo requestInfo, String tenantId) {
        if (StringUtils.isBlank(configuration.getIdGenCitizenIdName())) {
            throw new CustomException("INVALID_CONFIGURATION", "egov.idgen.citizen.id.name is not configured");
        }
        List<String> ids = getIdList(requestInfo, tenantId, configuration.getIdGenCitizenIdName(), configuration.getIdGenCitizenIdFormat(), 1);
        if (CollectionUtils.isEmpty(ids)) {
            throw new CustomException("IDGEN_ERROR", "Failed to generate citizen ID from idgen");
        }
        return ids.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateUserDocumentId(RequestInfo requestInfo, String tenantId) {
        if (StringUtils.isBlank(configuration.getIdGenUserDocIdName())) {
            throw new CustomException("INVALID_CONFIGURATION", "egov.idgen.user.doc.id.name is not configured");
        }
        List<String> ids = getIdList(requestInfo, tenantId, configuration.getIdGenUserDocIdName(), configuration.getIdGenUserDocIdFormat(), 1);
        if (CollectionUtils.isEmpty(ids)) {
            throw new CustomException("IDGEN_ERROR", "Failed to generate user document ID from idgen");
        }
        return ids.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<String> getIdList(RequestInfo requestInfo, String tenantId, String idName, String format, int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        if (configuration == null || StringUtils.isBlank(configuration.getIdGenHost())) {
            throw new CustomException("INVALID_CONFIGURATION", "egov.idgen.host is not configured");
        }
        if (StringUtils.isBlank(configuration.getIdGenPath())) {
            throw new CustomException("INVALID_CONFIGURATION", "egov.idgen.path is not configured");
        }
        if (StringUtils.isBlank(idName)) {
            throw new CustomException("INVALID_CONFIGURATION", "idName is mandatory for ID generation");
        }

        String rootTenant = StringUtils.isNotBlank(tenantId) ? tenantId.split("\\.")[0] : "pg";
        String url = buildUrl(configuration.getIdGenHost(), configuration.getIdGenPath());
        log.info("[EGOV-USER-IDGEN] Requesting ID generation for idName: {}, tenant: {}, count: {} from: {}",
                idName, rootTenant, count, url);

        List<Map<String, Object>> idRequests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> req = new HashMap<>();
            req.put("idName", idName);
            req.put("tenantId", rootTenant);
            if (StringUtils.isNotBlank(format)) {
                req.put("format", format);
            }
            idRequests.add(req);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("RequestInfo", requestInfo != null ? requestInfo : new RequestInfo());
        requestBody.put("idRequests", idRequests);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("idResponses")) {
                List<Map<String, Object>> idResponses = (List<Map<String, Object>>) response.get("idResponses");
                if (!CollectionUtils.isEmpty(idResponses)) {
                    List<String> generatedIds = idResponses.stream()
                            .map(r -> (String) r.get("id"))
                            .filter(StringUtils::isNotBlank)
                            .collect(Collectors.toList());
                    if (generatedIds.size() == count) {
                        log.info("[EGOV-USER-IDGEN] Successfully generated {} IDs for idName: {}", count, idName);
                        return generatedIds;
                    }
                }
            }
            throw new CustomException("IDGEN_ERROR", "Incomplete or empty ID responses received from IDGen for " + idName);
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[EGOV-USER-IDGEN] Failed to generate ID from IDGen for {}: {}", idName, e.getMessage());
            throw new CustomException("IDGEN_ERROR", "Error generating ID from idgen service: " + e.getMessage());
        }
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
