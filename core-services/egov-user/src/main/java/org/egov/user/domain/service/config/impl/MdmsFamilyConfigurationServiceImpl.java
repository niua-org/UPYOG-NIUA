package org.egov.user.domain.service.config.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.service.config.FamilyConfigurationService;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.config.FamilyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MDMS-backed implementation of {@link FamilyConfigurationService} for egov-user service.
 *
 * <p>Dynamically retrieves state-level Family Configuration from UPYOG MDMS service,
 * caches the active policy in memory, and validates incoming multi-auth and profile operations
 * against the configured state family governance model.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class MdmsFamilyConfigurationServiceImpl implements FamilyConfigurationService {

    private final RestTemplate restTemplate;
    private final UserConfiguration userConfiguration;
    private final ObjectMapper objectMapper;

    /**
     * In-memory cache entry encapsulating tenant family configuration with timestamp.
     */
    public static class CacheEntry {
        private final FamilyConfig familyConfig;
        private final long lastUpdatedTimestamp;

        public CacheEntry(FamilyConfig familyConfig, long lastUpdatedTimestamp) {
            this.familyConfig = familyConfig;
            this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        }

        /**
         * Returns the cached family configuration.
         *
         * @return cached {@link FamilyConfig}
         */
        public FamilyConfig getFamilyConfig() {
            return familyConfig;
        }

        /**
         * Checks whether this cache entry has exceeded the given time-to-live in milliseconds.
         *
         * @param ttlMillis cache time-to-live in milliseconds
         * @return true if expired, false otherwise
         */
        public boolean isExpired(long ttlMillis) {
            return (System.currentTimeMillis() - lastUpdatedTimestamp) > ttlMillis;
        }
    }

    private final Map<String, CacheEntry> tenantConfigCache = new ConcurrentHashMap<>();

    /**
     * Constructs a MdmsFamilyConfigurationServiceImpl with required dependencies.
     *
     * @param restTemplate      Spring RestTemplate for remote HTTP calls to MDMS
     * @param userConfiguration application configuration
     * @param objectMapper       Jackson ObjectMapper for JSON deserialization
     */
    @Autowired
    public MdmsFamilyConfigurationServiceImpl(RestTemplate restTemplate,
                                              UserConfiguration userConfiguration,
                                              ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.userConfiguration = userConfiguration;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Retrieves the active Family Configuration for the tenant from cache or UPYOG MDMS.
     * Falls back safely to default UPYOG_INTERNAL configuration if not yet configured in MDMS.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param tenantId    tenant identifier
     * @return active {@link FamilyConfig}
     */
    @Override
    public FamilyConfig getActiveFamilyConfiguration(RequestInfo requestInfo, String tenantId) {
        String stateTenant = resolveStateTenant(tenantId);
        long ttlMillis = (userConfiguration != null && userConfiguration.getFamilyConfigurationCacheTtl() != null)
                ? userConfiguration.getFamilyConfigurationCacheTtl()
                : 86400000L;

        CacheEntry entry = tenantConfigCache.get(stateTenant);
        if (entry != null && !entry.isExpired(ttlMillis)) {
            return entry.getFamilyConfig();
        }

        return refreshCache(requestInfo, stateTenant);
    }

    /**
     * Validates that the family concept is supported for the state/tenant.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param tenantId    target tenant identifier
     * @throws CustomException if family is not supported
     */
    @Override
    public void validateFamilySupported(RequestInfo requestInfo, String tenantId) {
        FamilyConfig config = getActiveFamilyConfiguration(requestInfo, tenantId);
        if (config != null && !config.isFamilySupported()) {
            log.error("[FAMILY-CONFIG-MDMS] Family concept is disabled for tenant {}", tenantId);
            throw new CustomException(UserConstants.ERROR_FAMILY_NOT_SUPPORTED,
                    "Family concept is disabled for tenant: " + tenantId);
        }
    }

    /**
     * Validates that the tenant supports UPYOG native internal family operations.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param tenantId    target tenant identifier
     * @throws CustomException if native family operations are not permitted
     */
    @Override
    public void validateUpyogNativeFamily(RequestInfo requestInfo, String tenantId) {
        validateFamilySupported(requestInfo, tenantId);
        FamilyConfig config = getActiveFamilyConfiguration(requestInfo, tenantId);
        if (config != null && config.isStateRegistry()) {
            log.error("[FAMILY-CONFIG-MDMS] Native family operations disabled for tenant {} because state uses {}",
                    tenantId, config.getFamilyType());
            throw new CustomException(UserConstants.ERROR_OPERATION_NOT_ALLOWED,
                    "Native family operations are disabled for tenant " + tenantId
                            + " because family data is managed by State Family Registry");
        }
    }

    /**
     * Validates that the tenant supports State Family Registry integration.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param tenantId    target tenant identifier
     * @throws CustomException if state family integration is not permitted
     */
    @Override
    public void validateStateRegistryFamily(RequestInfo requestInfo, String tenantId) {
        validateFamilySupported(requestInfo, tenantId);
        FamilyConfig config = getActiveFamilyConfiguration(requestInfo, tenantId);
        if (config != null && !config.isStateRegistry()) {
            log.error("[FAMILY-CONFIG-MDMS] State family action rejected: tenant {} is configured as {}",
                    tenantId, config.getFamilyType());
            throw new CustomException(UserConstants.ERROR_OPERATION_NOT_ALLOWED,
                    "State Family Integration is only supported when familyType is STATE_REGISTRY. Tenant "
                            + tenantId + " is configured as: " + config.getFamilyType());
        }
    }

    /**
     * Invalidates the in-memory cache across all tenants.
     */
    @Override
    public void clearCache() {
        log.info("[FAMILY-CONFIG-MDMS] Invalidate family configuration master cache across all tenants");
        tenantConfigCache.clear();
    }

    /**
     * Refreshes the family configuration cache for the given tenant by querying MDMS.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param tenantId    tenant identifier
     * @return refreshed {@link FamilyConfig}
     */
    @Override
    public FamilyConfig refreshCache(RequestInfo requestInfo, String tenantId) {
        String stateTenant = resolveStateTenant(tenantId);
        log.info("[FAMILY-CONFIG-MDMS] Fetching FamilyConfiguration master from UPYOG MDMS for tenant: {}", stateTenant);

        FamilyConfig config = fetchConfigFromMdms(requestInfo, stateTenant);
        if (config == null) {
            log.info("[FAMILY-CONFIG-MDMS] No active FamilyConfiguration in MDMS for tenant {}. Using default UPYOG_INTERNAL config.",
                    stateTenant);
            config = FamilyConfig.createDefault(stateTenant);
        }

        tenantConfigCache.put(stateTenant, new CacheEntry(config, System.currentTimeMillis()));
        return config;
    }

    /**
     * Queries MDMS for the active FamilyConfiguration record for the given state tenant.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param stateTenant state tenant identifier (e.g. "pg", "hr")
     * @return active FamilyConfig or null if not found
     */
    private FamilyConfig fetchConfigFromMdms(RequestInfo requestInfo, String stateTenant) {
        if (userConfiguration == null || StringUtils.isBlank(userConfiguration.getMdmsHost())
                || StringUtils.isBlank(userConfiguration.getMdmsPath())) {
            log.warn("[FAMILY-CONFIG-MDMS] MDMS host or search path not configured. Falling back to default.");
            return null;
        }

        String moduleName = StringUtils.isNotBlank(userConfiguration.getMdmsFamilyModule())
                ? userConfiguration.getMdmsFamilyModule()
                : "common-masters";
        String masterName = StringUtils.isNotBlank(userConfiguration.getMdmsFamilyConfigurationMaster())
                ? userConfiguration.getMdmsFamilyConfigurationMaster()
                : UserConstants.MDMS_MASTER_FAMILY_CONFIGURATION;
        String filter = StringUtils.isNotBlank(userConfiguration.getMdmsFamilyConfigurationFilter())
                ? userConfiguration.getMdmsFamilyConfigurationFilter()
                : "[?(@.active==true)]";

        String host = userConfiguration.getMdmsHost().trim();
        String path = userConfiguration.getMdmsPath().trim();
        String url = host.endsWith("/") ? host + path : host + "/" + path;

        MasterDetail masterDetail = MasterDetail.builder()
                .name(masterName)
                .filter(filter)
                .build();

        ModuleDetail moduleDetail = ModuleDetail.builder()
                .moduleName(moduleName)
                .masterDetails(Collections.singletonList(masterDetail))
                .build();

        MdmsCriteria criteria = new MdmsCriteria();
        criteria.setTenantId(stateTenant);
        criteria.setModuleDetails(Collections.singletonList(moduleDetail));

        MdmsCriteriaReq criteriaReq = new MdmsCriteriaReq();
        criteriaReq.setRequestInfo(requestInfo != null ? requestInfo : new RequestInfo());
        criteriaReq.setMdmsCriteria(criteria);

        try {
            JsonNode responseNode = restTemplate.postForObject(url, criteriaReq, JsonNode.class);
            if (responseNode != null) {
                JsonNode items = responseNode.path("MdmsRes").path(moduleName).path(masterName);
                if (items.isArray() && items.size() > 0) {
                    JsonNode firstItem = items.get(0);
                    FamilyConfig parsedConfig = objectMapper.treeToValue(firstItem, FamilyConfig.class);
                    log.info("[FAMILY-CONFIG-MDMS] Successfully loaded FamilyConfiguration from MDMS for tenant {}: type={}, isSupported={}",
                            stateTenant, parsedConfig.getFamilyType(), parsedConfig.getIsFamilySupported());
                    return parsedConfig;
                }
            }
        } catch (Exception e) {
            log.warn("[FAMILY-CONFIG-MDMS] Error calling MDMS service at {} for tenant {}: {}. Falling back to default config.",
                    url, stateTenant, e.getMessage());
        }

        return null;
    }

    /**
     * Resolves the state-level tenant identifier from a city or sub-tenant string (e.g. 'pg.citya' -> 'pg').
     */
    private String resolveStateTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            if (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getStateLevelTenantId())) {
                return userConfiguration.getStateLevelTenantId().trim();
            }
            throw new CustomException(UserConstants.ERROR_TENANT_ID_REQUIRED, "tenantId is mandatory");
        }
        return tenantId.contains(".") ? tenantId.split("\\.")[0] : tenantId;
    }
}
