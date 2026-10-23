package org.upyog.family.domain.service.master;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.tracer.model.CustomException;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.util.FamilyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service implementation managing dynamic family master data retrieval from UPYOG MDMS.
 *
 * <p>Enforces dynamic runtime validation of member relationships, eliminating hardcoded enums
 * and enabling instant master data updates in MDMS without requiring service redeployment.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class FamilyMasterDataServiceImpl implements FamilyMasterDataService {

    private final RestTemplate restTemplate;
    private final FamilyConfiguration configuration;

    private static final Set<String> DEFAULT_RELATIONSHIPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "HEAD", "SPOUSE", "SON", "DAUGHTER", "FATHER", "MOTHER", "BROTHER", "SISTER",
            "GRANDFATHER", "GRANDMOTHER", "GRANDSON", "GRANDDAUGHTER", "DAUGHTER_IN_LAW", "SON_IN_LAW", "OTHER"
    )));

    public static class CacheEntry {
        private final Set<String> allowedRelationships;
        private final long lastUpdatedTimestamp;

        public CacheEntry(Set<String> allowedRelationships, long lastUpdatedTimestamp) {
            this.allowedRelationships = Collections.unmodifiableSet(new HashSet<>(allowedRelationships));
            this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        }

        public Set<String> getAllowedRelationships() {
            return allowedRelationships;
        }

        public boolean isExpired(long ttlMillis) {
            return (System.currentTimeMillis() - lastUpdatedTimestamp) > ttlMillis;
        }
    }

    private final Map<String, CacheEntry> tenantRelationshipCache = new ConcurrentHashMap<>();

    @Autowired
    public FamilyMasterDataServiceImpl(RestTemplate restTemplate, FamilyConfiguration configuration) {
        this.restTemplate = restTemplate;
        this.configuration = configuration;
    }

    @Override
    public Set<String> getAllowedRelationships(RequestInfo requestInfo, String tenantId) {
        String stateTenant = resolveStateTenant(tenantId);
        long ttlMillis = configuration.getRelationshipCacheTtl() != null
                ? configuration.getRelationshipCacheTtl()
                : 86400000L;

        CacheEntry entry = tenantRelationshipCache.get(stateTenant);
        if (entry != null && !entry.isExpired(ttlMillis)) {
            return entry.getAllowedRelationships();
        }

        return refreshCache(requestInfo, stateTenant);
    }

    @Override
    public void validateRelationship(RequestInfo requestInfo, String tenantId, String relationship) {
        if (StringUtils.isBlank(relationship)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_RELATIONSHIP, "Relationship with Head of Family cannot be blank");
        }

        Set<String> allowed = getAllowedRelationships(requestInfo, tenantId);
        if (!allowed.contains(relationship.trim().toUpperCase())) {
            log.error("[UPYOG-FAMILY-MASTER] Invalid relationship '{}' for tenant '{}'. Allowed: {}",
                    relationship, tenantId, allowed);
            throw new CustomException(FamilyConstants.ERROR_INVALID_RELATIONSHIP,
                    "Invalid relationship: '" + relationship + "'. Must be one of: " + allowed);
        }
    }

    @Override
    public void clearCache() {
        log.info("[UPYOG-FAMILY-MASTER] Invalidate relationship master cache across all tenants");
        tenantRelationshipCache.clear();
    }

    /**
     * Invalidates and fetches fresh FamilyRelationship master data from UPYOG MDMS for a tenant.
     * Updates the in-memory cache and returns the refreshed set of relationship codes.
     *
     * @param requestInfo UPYOG request context envelope
     * @param tenantId target tenant identifier
     * @return refreshed set of valid uppercase relationship codes
     */
    @Override
    public Set<String> refreshCache(RequestInfo requestInfo, String tenantId) {
        String stateTenant = resolveStateTenant(tenantId);
        log.info("[UPYOG-FAMILY-MASTER] Fetching FamilyRelationship master from UPYOG MDMS for tenant: {}", stateTenant);

        Set<String> relationships = fetchRelationshipsFromMdms(requestInfo, stateTenant);
        if (relationships == null || relationships.isEmpty()) {
            log.warn("[UPYOG-FAMILY-MASTER] MDMS returned empty relationships. Using default UPYOG relationships.");
            relationships = DEFAULT_RELATIONSHIPS;
        }

        tenantRelationshipCache.put(stateTenant, new CacheEntry(relationships, System.currentTimeMillis()));
        return relationships;
    }

    private Set<String> fetchRelationshipsFromMdms(RequestInfo requestInfo, String stateTenant) {
        String url = configuration.getMdmsHost() + configuration.getMdmsSearchPath();
        MdmsCriteriaReq request = buildMdmsRequest(requestInfo, stateTenant);

        try {
            Object response = restTemplate.postForObject(url, request, Map.class);
            if (response != null) {
                String jsonPath = "$.MdmsRes." + configuration.getMdmsFamilyModule() + "."
                        + configuration.getMdmsRelationshipMaster() + "[*].code";
                List<String> rawCodes = JsonPath.read(response, jsonPath);
                if (rawCodes != null && !rawCodes.isEmpty()) {
                    Set<String> normalized = rawCodes.stream()
                            .filter(StringUtils::isNotBlank)
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .collect(Collectors.toSet());
                    log.info("[UPYOG-FAMILY-MASTER] Successfully loaded {} relationships from UPYOG MDMS: {}",
                            normalized.size(), normalized);
                    return normalized;
                }
            }
        } catch (Exception e) {
            log.warn("[UPYOG-FAMILY-MASTER] Error calling UPYOG MDMS service at {}: {}. Falling back to default taxonomy.",
                    url, e.getMessage());
        }

        return Collections.emptySet();
    }

    private MdmsCriteriaReq buildMdmsRequest(RequestInfo requestInfo, String stateTenant) {
        MasterDetail masterDetail = MasterDetail.builder()
                .name(configuration.getMdmsRelationshipMaster())
                .filter(configuration.getMdmsRelationshipFilter())
                .build();

        ModuleDetail moduleDetail = ModuleDetail.builder()
                .moduleName(configuration.getMdmsFamilyModule())
                .masterDetails(Collections.singletonList(masterDetail))
                .build();

        MdmsCriteria criteria = MdmsCriteria.builder()
                .tenantId(stateTenant)
                .moduleDetails(Collections.singletonList(moduleDetail))
                .build();

        return MdmsCriteriaReq.builder()
                .requestInfo(requestInfo != null ? requestInfo : new RequestInfo())
                .mdmsCriteria(criteria)
                .build();
    }

    private String resolveStateTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return "pg";
        }
        return tenantId.contains(".") ? tenantId.split("\\.")[0] : tenantId;
    }
}
