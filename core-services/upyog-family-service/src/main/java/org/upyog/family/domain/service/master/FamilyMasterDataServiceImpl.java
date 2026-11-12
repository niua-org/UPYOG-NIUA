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

    /**
     * Immutable cache entry containing allowed relationship codes and entry creation timestamp.
     */
    public static class CacheEntry {
        private final Set<String> allowedRelationships;
        private final long lastUpdatedTimestamp;

        /**
         * Constructs a new CacheEntry with allowed relationships and timestamp.
         *
         * @param allowedRelationships set of allowed uppercase relationship codes
         * @param lastUpdatedTimestamp timestamp when the cache entry was created
         */
        public CacheEntry(Set<String> allowedRelationships, long lastUpdatedTimestamp) {
            this.allowedRelationships = Collections.unmodifiableSet(new HashSet<>(allowedRelationships));
            this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        }

        /**
         * Gets the unmodifiable set of allowed relationship codes.
         *
         * @return set of relationship codes
         */
        public Set<String> getAllowedRelationships() {
            return allowedRelationships;
        }

        /**
         * Checks if this cache entry has expired beyond the configured TTL in milliseconds.
         *
         * @param ttlMillis time-to-live threshold in milliseconds
         * @return true if current time exceeds timestamp + ttlMillis, false otherwise
         */
        public boolean isExpired(long ttlMillis) {
            return (System.currentTimeMillis() - lastUpdatedTimestamp) > ttlMillis;
        }
    }

    private final Map<String, CacheEntry> tenantRelationshipCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> tenantCategoryCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> tenantOccupationCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> tenantPropertyTypeCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> tenantPropertyUsageCache = new ConcurrentHashMap<>();

    /**
     * Constructs a FamilyMasterDataServiceImpl with RestTemplate and configuration.
     *
     * @param restTemplate Spring RestTemplate for remote HTTP calls to MDMS
     * @param configuration family module configuration for MDMS endpoints and TTLs
     */
    @Autowired
    public FamilyMasterDataServiceImpl(RestTemplate restTemplate, FamilyConfiguration configuration) {
        this.restTemplate = restTemplate;
        this.configuration = configuration;
    }

    /**
     * Retrieves allowed relationship codes for the specified tenant from cache or UPYOG MDMS.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param tenantId tenant identifier
     * @return set of valid uppercase relationship codes
     */
    @Override
    public Set<String> getAllowedRelationships(RequestInfo requestInfo, String tenantId) {
        String stateTenant = resolveStateTenant(tenantId);
        if (configuration == null || configuration.getRelationshipCacheTtl() == null) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_RELATIONSHIP_CACHE_TTL_NOT_CONFIGURED);
        }
        long ttlMillis = configuration.getRelationshipCacheTtl();

        CacheEntry entry = tenantRelationshipCache.get(stateTenant);
        if (entry != null && !entry.isExpired(ttlMillis)) {
            return entry.getAllowedRelationships();
        }

        return refreshCache(requestInfo, stateTenant);
    }

    /**
     * Validates that the provided relationship string is valid according to MDMS taxonomy for the tenant.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param tenantId tenant identifier
     * @param relationship relationship string to validate against MDMS taxonomy
     * @throws CustomException if relationship is blank or not in allowed set
     */
    @Override
    public void validateRelationship(RequestInfo requestInfo, String tenantId, String relationship) {
        if (StringUtils.isBlank(relationship)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_RELATIONSHIP, FamilyConstants.MSG_RELATIONSHIP_CANNOT_BE_BLANK);
        }

        Set<String> allowed = getAllowedRelationships(requestInfo, tenantId);
        if (!allowed.contains(relationship.trim().toUpperCase())) {
            log.error("[UPYOG-FAMILY-MASTER] Invalid relationship '{}' for tenant '{}'. Allowed: {}",
                    relationship, tenantId, allowed);
            throw new CustomException(FamilyConstants.ERROR_INVALID_RELATIONSHIP,
                    FamilyConstants.MSG_INVALID_RELATIONSHIP_PREFIX + relationship + FamilyConstants.MSG_MUST_BE_ONE_OF_SUFFIX + allowed);
        }
    }

    /**
     * Retrieves the set of allowed socio-economic category codes configured in MDMS for the specified tenant.
     *
     * @param requestInfo incoming request metadata
     * @param tenantId tenant identifier
     * @return set of valid upper-case category codes
     */
    @Override
    public Set<String> getAllowedCategories(RequestInfo requestInfo, String tenantId) {
        String stateTenant = resolveStateTenant(tenantId);
        long ttlMillis = (configuration != null && configuration.getRelationshipCacheTtl() != null)
                ? configuration.getRelationshipCacheTtl() : 86400000L;
        CacheEntry entry = tenantCategoryCache.get(stateTenant);
        if (entry != null && !entry.isExpired(ttlMillis)) {
            return entry.getAllowedRelationships();
        }
        Set<String> fromMdms = fetchMasterCodesFromMdms(requestInfo, stateTenant,
                FamilyConstants.MDMS_MODULE_COMMON_MASTERS, FamilyConstants.MDMS_MASTER_FAMILY_CATEGORY);
        Set<String> categories = (fromMdms != null) ? fromMdms : Collections.emptySet();
        tenantCategoryCache.put(stateTenant, new CacheEntry(categories, System.currentTimeMillis()));
        return categories;
    }

    /**
     * Validates whether the supplied socio-economic category is defined within MDMS for the tenant.
     *
     * @param requestInfo incoming request metadata
     * @param tenantId tenant identifier
     * @param category category code to validate
     * @throws CustomException if category is invalid
     */
    @Override
    public void validateCategory(RequestInfo requestInfo, String tenantId, String category) {
        if (StringUtils.isNotBlank(category)) {
            String normalized = category.trim().toUpperCase();
            Set<String> allowed = getAllowedCategories(requestInfo, tenantId);
            if (!allowed.contains(normalized)) {
                log.error("[UPYOG-FAMILY-MASTER] Invalid category '{}' for tenant '{}'. Allowed: {}",
                        category, tenantId, allowed);
                throw new CustomException(FamilyConstants.ERROR_INVALID_CATEGORY,
                        FamilyConstants.MSG_INVALID_CATEGORY_PREFIX + category + FamilyConstants.MSG_ALLOWED_SUFFIX + allowed);
            }
        }
    }

    /**
     * Retrieves the set of allowed occupation codes configured in MDMS for the specified tenant.
     *
     * @param requestInfo incoming request metadata
     * @param tenantId tenant identifier
     * @return set of valid upper-case occupation codes
     */
    @Override
    public Set<String> getAllowedOccupations(RequestInfo requestInfo, String tenantId) {
        String stateTenant = resolveStateTenant(tenantId);
        long ttlMillis = (configuration != null && configuration.getRelationshipCacheTtl() != null)
                ? configuration.getRelationshipCacheTtl() : 86400000L;
        CacheEntry entry = tenantOccupationCache.get(stateTenant);
        if (entry != null && !entry.isExpired(ttlMillis)) {
            return entry.getAllowedRelationships();
        }
        Set<String> fromMdms = fetchMasterCodesFromMdms(requestInfo, stateTenant,
                FamilyConstants.MDMS_MODULE_COMMON_MASTERS, FamilyConstants.MDMS_MASTER_OCCUPATION);
        Set<String> occupations = (fromMdms != null) ? fromMdms : Collections.emptySet();
        tenantOccupationCache.put(stateTenant, new CacheEntry(occupations, System.currentTimeMillis()));
        return occupations;
    }

    /**
     * Validates whether the supplied occupation is defined within MDMS for the tenant.
     *
     * @param requestInfo incoming request metadata
     * @param tenantId tenant identifier
     * @param occupation occupation code to validate
     * @throws CustomException if occupation is invalid
     */
    @Override
    public void validateOccupation(RequestInfo requestInfo, String tenantId, String occupation) {
        if (StringUtils.isNotBlank(occupation)) {
            Set<String> allowed = getAllowedOccupations(requestInfo, tenantId);
            if (!allowed.contains(occupation.trim().toUpperCase())) {
                log.error("[UPYOG-FAMILY-MASTER] Invalid occupation '{}' for tenant '{}'. Allowed: {}",
                        occupation, tenantId, allowed);
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                        FamilyConstants.MSG_INVALID_OCCUPATION_PREFIX + occupation + FamilyConstants.MSG_ALLOWED_SUFFIX + allowed);
            }
        }
    }

    /**
     * Retrieves the set of allowed property types configured in MDMS for the specified tenant.
     *
     * @param requestInfo incoming request metadata
     * @param tenantId tenant identifier
     * @return set of valid upper-case property types
     */
    @Override
    public Set<String> getAllowedPropertyTypes(RequestInfo requestInfo, String tenantId) {
        String stateTenant = resolveStateTenant(tenantId);
        long ttlMillis = (configuration != null && configuration.getRelationshipCacheTtl() != null)
                ? configuration.getRelationshipCacheTtl() : 86400000L;
        CacheEntry entry = tenantPropertyTypeCache.get(stateTenant);
        if (entry != null && !entry.isExpired(ttlMillis)) {
            return entry.getAllowedRelationships();
        }
        Set<String> fromMdms = fetchMasterCodesFromMdms(requestInfo, stateTenant,
                FamilyConstants.MDMS_MODULE_PROPERTY_TAX, FamilyConstants.MDMS_MASTER_PROPERTY_TYPE);
        Set<String> propertyTypes = (fromMdms != null) ? fromMdms : Collections.emptySet();
        tenantPropertyTypeCache.put(stateTenant, new CacheEntry(propertyTypes, System.currentTimeMillis()));
        return propertyTypes;
    }

    /**
     * Validates whether the supplied property type is defined within MDMS for the tenant.
     *
     * @param requestInfo incoming request metadata
     * @param tenantId tenant identifier
     * @param propertyType property type to validate
     * @throws CustomException if property type is invalid
     */
    @Override
    public void validatePropertyType(RequestInfo requestInfo, String tenantId, String propertyType) {
        if (StringUtils.isNotBlank(propertyType)) {
            Set<String> allowed = getAllowedPropertyTypes(requestInfo, tenantId);
            if (!allowed.contains(propertyType.trim().toUpperCase())) {
                log.error("[UPYOG-FAMILY-MASTER] Invalid propertyType '{}' for tenant '{}'. Allowed: {}",
                        propertyType, tenantId, allowed);
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                        FamilyConstants.MSG_INVALID_PROPERTY_TYPE_PREFIX + propertyType + FamilyConstants.MSG_ALLOWED_SUFFIX + allowed);
            }
        }
    }

    /**
     * Retrieves the set of allowed property usage categories configured in MDMS for the specified tenant.
     *
     * @param requestInfo incoming request metadata
     * @param tenantId tenant identifier
     * @return set of valid upper-case property usage categories
     */
    @Override
    public Set<String> getAllowedPropertyUsages(RequestInfo requestInfo, String tenantId) {
        String stateTenant = resolveStateTenant(tenantId);
        long ttlMillis = (configuration != null && configuration.getRelationshipCacheTtl() != null)
                ? configuration.getRelationshipCacheTtl() : 86400000L;
        CacheEntry entry = tenantPropertyUsageCache.get(stateTenant);
        if (entry != null && !entry.isExpired(ttlMillis)) {
            return entry.getAllowedRelationships();
        }
        Set<String> fromMdms = fetchMasterCodesFromMdms(requestInfo, stateTenant,
                FamilyConstants.MDMS_MODULE_PROPERTY_TAX, FamilyConstants.MDMS_MASTER_USAGE_CATEGORY);
        Set<String> propertyUsages = (fromMdms != null) ? fromMdms : Collections.emptySet();
        tenantPropertyUsageCache.put(stateTenant, new CacheEntry(propertyUsages, System.currentTimeMillis()));
        return propertyUsages;
    }

    /**
     * Validates whether the supplied property usage category is defined within MDMS for the tenant.
     *
     * @param requestInfo incoming request metadata
     * @param tenantId tenant identifier
     * @param propertyUsage property usage category to validate
     * @throws CustomException if property usage category is invalid
     */
    @Override
    public void validatePropertyUsage(RequestInfo requestInfo, String tenantId, String propertyUsage) {
        if (StringUtils.isNotBlank(propertyUsage)) {
            Set<String> allowed = getAllowedPropertyUsages(requestInfo, tenantId);
            if (!allowed.contains(propertyUsage.trim().toUpperCase())) {
                log.error("[UPYOG-FAMILY-MASTER] Invalid propertyUsage '{}' for tenant '{}'. Allowed: {}",
                        propertyUsage, tenantId, allowed);
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                        FamilyConstants.MSG_INVALID_PROPERTY_USAGE_PREFIX + propertyUsage + FamilyConstants.MSG_ALLOWED_SUFFIX + allowed);
            }
        }
    }

    /**
     * Evicts cached relationship, occupation, and property sets across all tenants.
     */
    @Override
    public void clearCache() {
        log.info("[UPYOG-FAMILY-MASTER] Invalidate master caches across all tenants");
        tenantRelationshipCache.clear();
        tenantCategoryCache.clear();
        tenantOccupationCache.clear();
        tenantPropertyTypeCache.clear();
        tenantPropertyUsageCache.clear();
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
            log.warn("[UPYOG-FAMILY-MASTER] MDMS returned empty relationships for tenant: {}", stateTenant);
            relationships = Collections.emptySet();
        }

        tenantRelationshipCache.put(stateTenant, new CacheEntry(relationships, System.currentTimeMillis()));
        return relationships;
    }

    /**
     * Executes MDMS search request to fetch master codes for the specified module and master name.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param stateTenant state tenant identifier (e.g. "pg")
     * @param moduleName MDMS module name (e.g. "common-masters", "PropertyTax")
     * @param masterName MDMS master name (e.g. "Occupation", "PropertyType", "UsageCategory")
     * @return set of normalized uppercase codes, or empty set on failure
     */
    private Set<String> fetchMasterCodesFromMdms(RequestInfo requestInfo, String stateTenant, String moduleName, String masterName) {
        if (configuration == null || StringUtils.isBlank(configuration.getMdmsHost()) || StringUtils.isBlank(configuration.getMdmsSearchPath())) {
            log.warn("[UPYOG-FAMILY-MASTER] MDMS host or search path is not configured");
            return Collections.emptySet();
        }
        String url = configuration.getMdmsHost() + configuration.getMdmsSearchPath();

        MasterDetail masterDetail = MasterDetail.builder()
                .name(masterName)
                .filter("[?(@.active==true)]")
                .build();

        ModuleDetail moduleDetail = ModuleDetail.builder()
                .moduleName(moduleName)
                .masterDetails(Collections.singletonList(masterDetail))
                .build();

        MdmsCriteria criteria = MdmsCriteria.builder()
                .tenantId(stateTenant)
                .moduleDetails(Collections.singletonList(moduleDetail))
                .build();

        MdmsCriteriaReq request = MdmsCriteriaReq.builder()
                .requestInfo(requestInfo != null ? requestInfo : new RequestInfo())
                .mdmsCriteria(criteria)
                .build();

        try {
            Object response = restTemplate.postForObject(url, request, Map.class);
            if (response != null) {
                String jsonPath = "$.MdmsRes." + moduleName + "." + masterName + "[*].code";
                List<String> rawCodes = JsonPath.read(response, jsonPath);
                if (rawCodes != null && !rawCodes.isEmpty()) {
                    Set<String> normalized = rawCodes.stream()
                            .filter(StringUtils::isNotBlank)
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .collect(Collectors.toSet());
                    log.info("[UPYOG-FAMILY-MASTER] Successfully loaded {} codes from MDMS for {}.{}: {}",
                            normalized.size(), moduleName, masterName, normalized);
                    return normalized;
                }
            }
        } catch (Exception e) {
            log.warn("[UPYOG-FAMILY-MASTER] Failed to fetch {}.{} from MDMS at {}: {}. Falling back to default.",
                    moduleName, masterName, url, e.getMessage());
        }

        return Collections.emptySet();
    }

    /**
     * Executes MDMS search request to fetch family relationships configured for the state tenant.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param stateTenant state tenant identifier (e.g. "pg")
     * @return set of normalized relationship codes, or empty set on failure
     */
    private Set<String> fetchRelationshipsFromMdms(RequestInfo requestInfo, String stateTenant) {
        if (configuration == null || StringUtils.isBlank(configuration.getMdmsHost())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_MDMS_HOST_NOT_CONFIGURED);
        }
        if (StringUtils.isBlank(configuration.getMdmsSearchPath())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_MDMS_SEARCH_PATH_NOT_CONFIGURED);
        }
        if (StringUtils.isBlank(configuration.getMdmsFamilyModule())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_FAMILY_MDMS_MODULE_NOT_CONFIGURED);
        }
        if (StringUtils.isBlank(configuration.getMdmsRelationshipMaster())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_FAMILY_MDMS_RELATIONSHIP_MASTER_NOT_CONFIGURED);
        }
        return fetchMasterCodesFromMdms(requestInfo, stateTenant, configuration.getMdmsFamilyModule(), configuration.getMdmsRelationshipMaster());
    }

    /**
     * Constructs the MDMS criteria request payload targeting the Family module's relationship master.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param stateTenant state tenant identifier
     * @return populated MdmsCriteriaReq object
     */
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

    /**
     * Resolves the state-level tenant identifier from a city or sub-tenant string (e.g., 'pg.citya' -> 'pg').
     *
     * @param tenantId input tenant identifier
     * @return resolved state tenant code, default 'pg' if blank
     */
    private String resolveStateTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_TENANT_ID_MANDATORY);
        }
        return tenantId.contains(".") ? tenantId.split("\\.")[0] : tenantId;
    }
}
