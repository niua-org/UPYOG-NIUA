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
     * Calls UPYOG IdGen microservice to generate a formatted, unique sequential Family ID.
     *
     * @param requestInfo UPYOG request context envelope containing user session and auth token
     * @param tenantId    target tenant identifier scoping the sequence
     * @return generated formatted unique Family ID (e.g. {@code FAM-2026-000001})
     * @throws CustomException if {@code egov.idgen.family.id.name} is missing or generation fails
     */
    @Override
    public String generateFamilyId(RequestInfo requestInfo, String tenantId) {
        if (StringUtils.isBlank(configuration.getIdGenFamilyIdName())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_IDGEN_FAMILY_ID_NOT_CONFIGURED);
        }
        List<String> ids = getIdList(requestInfo, tenantId, configuration.getIdGenFamilyIdName(), null, 1);
        if (CollectionUtils.isEmpty(ids)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_IDGEN_FAMILY_ID_FAILED);
        }
        return ids.get(0);
    }

    /**
     * Calls UPYOG IdGen microservice to generate a formatted, unique sequential Application Number.
     *
     * <p>Constructs and dispatches an ID generation request targeting the configured
     * {@code family.application.num} IdGen format template within the specified tenant.</p>
     *
     * @param requestInfo UPYOG request context envelope containing user session and auth token
     * @param tenantId    target tenant identifier scoping the sequence (e.g. {@code pg.citya})
     * @return generated formatted unique Application Number (e.g. {@code FAM-APP-2026-000001})
     * @throws CustomException if {@code egov.idgen.family.application.num} is missing or generation fails
     */
    @Override
    public String generateApplicationNo(RequestInfo requestInfo, String tenantId) {
        if (StringUtils.isBlank(configuration.getIdGenApplicationNum())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_IDGEN_APPLICATION_NUM_NOT_CONFIGURED);
        }
        List<String> ids = getIdList(requestInfo, tenantId, configuration.getIdGenApplicationNum(), null, 1);
        if (CollectionUtils.isEmpty(ids)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_IDGEN_APPLICATION_NUM_FAILED);
        }
        return ids.get(0);
    }

    /**
     * Calls UPYOG IdGen microservice to generate a formatted, unique sequential Family Member ID.
     *
     * @param requestInfo UPYOG request context envelope
     * @param tenantId    target tenant identifier
     * @return generated unique Family Member ID string
     * @throws CustomException if generation produces no IDs
     */
    @Override
    public String generateMemberId(RequestInfo requestInfo, String tenantId) {
        List<String> ids = generateMemberIds(requestInfo, tenantId, 1);
        if (CollectionUtils.isEmpty(ids)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_IDGEN_MEMBER_ID_FAILED);
        }
        return ids.get(0);
    }

    /**
     * Calls UPYOG IdGen microservice in bulk to generate formatted, unique sequential Family Member IDs.
     *
     * @param requestInfo UPYOG request context envelope
     * @param tenantId    target tenant identifier
     * @param count       number of member IDs to generate
     * @return list of generated unique Family Member ID strings
     * @throws CustomException if {@code egov.idgen.member.id.name} is missing or generation fails
     */
    @Override
    public List<String> generateMemberIds(RequestInfo requestInfo, String tenantId, int count) {
        if (StringUtils.isBlank(configuration.getIdGenMemberIdName())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_IDGEN_MEMBER_ID_NOT_CONFIGURED);
        }
        return getIdList(requestInfo, tenantId, configuration.getIdGenMemberIdName(), null, count);
    }

    /**
     * Generic bulk ID generation method querying UPYOG IdGen microservice.
     *
     * @param requestInfo UPYOG request context envelope
     * @param tenantId    target tenant identifier
     * @param idName      configured MDMS idName
     * @param format      optional format template override
     * @param count       number of IDs to generate
     * @return list of generated ID strings
     * @throws CustomException if idgen host is missing or response is invalid
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<String> getIdList(RequestInfo requestInfo, String tenantId, String idName, String format, int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        if (configuration == null || StringUtils.isBlank(configuration.getIdGenHost())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_IDGEN_HOST_NOT_CONFIGURED);
        }
        if (StringUtils.isBlank(configuration.getIdGenPath())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "egov.idgen.path is not configured");
        }
        if (StringUtils.isBlank(idName)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "idName is mandatory for ID generation");
        }
        if (StringUtils.isBlank(tenantId)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "tenantId is mandatory for ID generation");
        }

        String rootTenant = tenantId.split("\\.")[0];
        String url = buildUrl(configuration.getIdGenHost(), configuration.getIdGenPath());
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
