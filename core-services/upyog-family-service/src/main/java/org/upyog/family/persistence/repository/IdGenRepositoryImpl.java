package org.upyog.family.persistence.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.egov.common.contract.request.RequestInfo;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.util.FamilyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final FamilyConfiguration configuration;

    /**
     * Constructs an IdGenRepositoryImpl with RestTemplate and FamilyConfiguration dependencies.
     *
     * @param restTemplate Spring RestTemplate for making HTTP requests to egovidgen
     * @param configuration family module configuration for idgen host and format templates
     */
    @Autowired
    public IdGenRepositoryImpl(RestTemplate restTemplate, FamilyConfiguration configuration) {
        this.restTemplate = restTemplate;
        this.configuration = configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateFamilyId(RequestInfo requestInfo, String tenantId) {
        if (StringUtils.isBlank(configuration.getIdGenFamilyIdName())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "egov.idgen.family.id.name is not configured");
        }
        List<String> ids = getIdList(requestInfo, tenantId, configuration.getIdGenFamilyIdName(), configuration.getIdGenFamilyIdFormat(), 1);
        if (CollectionUtils.isEmpty(ids)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "Failed to generate family ID from idgen");
        }
        return ids.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateMemberId(RequestInfo requestInfo, String tenantId) {
        List<String> ids = generateMemberIds(requestInfo, tenantId, 1);
        if (CollectionUtils.isEmpty(ids)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "Failed to generate member ID from idgen");
        }
        return ids.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> generateMemberIds(RequestInfo requestInfo, String tenantId, int count) {
        if (StringUtils.isBlank(configuration.getIdGenMemberIdName())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "egov.idgen.member.id.name is not configured");
        }
        return getIdList(requestInfo, tenantId, configuration.getIdGenMemberIdName(), configuration.getIdGenMemberIdFormat(), count);
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
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "egov.idgen.host is not configured");
        }
        if (StringUtils.isBlank(configuration.getIdGenPath())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "egov.idgen.path is not configured");
        }
        if (StringUtils.isBlank(idName)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "idName is mandatory for ID generation");
        }

        String rootTenant = StringUtils.isNotBlank(tenantId) ? tenantId.split("\\.")[0] : "pg";
        String url = configuration.getIdGenHost() + configuration.getIdGenPath();
        log.info("[UPYOG-IDGEN-CLIENT] Requesting ID generation for idName: {}, tenant: {}, count: {} from: {}",
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
                        log.info("[UPYOG-IDGEN-CLIENT] Successfully generated {} IDs for idName: {}", count, idName);
                        return generatedIds;
                    }
                }
            }
            throw new CustomException("IDGEN_ERROR", "Incomplete or empty ID responses received from IDGen for " + idName);
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[UPYOG-IDGEN-CLIENT] Failed to generate ID from IDGen for {}: {}", idName, e.getMessage());
            throw new CustomException("IDGEN_ERROR", "Error generating ID from idgen service: " + e.getMessage());
        }
    }
}
