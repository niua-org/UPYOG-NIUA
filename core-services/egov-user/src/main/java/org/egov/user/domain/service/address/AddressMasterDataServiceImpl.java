package org.egov.user.domain.service.address;

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
import org.egov.user.domain.service.utils.UserConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AddressMasterDataService} dynamically fetching and validating
 * Address Types from the UPYOG MDMS service with automatic fallback to {@code application.properties}.
 *
 * <p>Enforces the <b>Single Responsibility Principle (SRP)</b> by isolating master data retrieval,
 * daily caching, and validation away from core address CRUD persistence logic.</p>
 *
 * <p>Uses a 24-hour time-to-live (TTL) in-memory cache along with a daily scheduled cron task
 * ({@code @Scheduled(cron = "${address.type.cache.cron}")}) to ensure MDMS is hit at most once per day,
 * while providing manual cache eviction/refresh APIs for administrative operations.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class AddressMasterDataServiceImpl implements AddressMasterDataService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserConfiguration userConfiguration;

    /**
     * Internal cache entry encapsulating allowed address types alongside fetch timestamp.
     */
    public static class CacheEntry {
        private final Set<String> allowedTypes;
        private final long lastUpdatedTimestamp;

        public CacheEntry(Set<String> allowedTypes, long lastUpdatedTimestamp) {
            this.allowedTypes = Collections.unmodifiableSet(new HashSet<>(allowedTypes));
            this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        }

        public Set<String> getAllowedTypes() {
            return allowedTypes;
        }

        public long getLastUpdatedTimestamp() {
            return lastUpdatedTimestamp;
        }

        public boolean isExpired(long ttlMillis) {
            return (System.currentTimeMillis() - lastUpdatedTimestamp) > ttlMillis;
        }
    }

    /** Cache of allowed address types per tenant to minimize MDMS network overhead */
    private final Map<String, CacheEntry> tenantAddressTypeCache = new ConcurrentHashMap<>();

    @Autowired
    public AddressMasterDataServiceImpl(RestTemplate restTemplate,
                                      ObjectMapper objectMapper,
                                      UserConfiguration userConfiguration) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userConfiguration = userConfiguration;
    }

    /**
     * Backward-compatible constructor for unit testing scenarios.
     *
     * @param restTemplate RestTemplate client
     * @param objectMapper ObjectMapper instance
     */
    public AddressMasterDataServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this(restTemplate, objectMapper, null);
    }

    @Override
    public Set<String> getAllowedAddressTypes(RequestInfo requestInfo, String tenantId) {
        String stateTenant = getStateLevelTenant(tenantId);
        long ttlMillis = (userConfiguration != null && userConfiguration.getAddressTypeCacheTtlMillis() != null)
                ? userConfiguration.getAddressTypeCacheTtlMillis()
                : 86400000L; // Default 24 hours

        CacheEntry entry = tenantAddressTypeCache.get(stateTenant);
        if (entry != null && !entry.isExpired(ttlMillis)) {
            log.debug("[ADDRESS-MDMS] Returning cached address types for tenant {}: {}", stateTenant, entry.getAllowedTypes());
            return entry.getAllowedTypes();
        }

        Set<String> allowedTypes = new HashSet<>();
        boolean mdmsEnabled = userConfiguration == null || Boolean.TRUE.equals(userConfiguration.getMdmsAddressTypeEnabled());
        String mdmsHost = userConfiguration != null ? userConfiguration.getMdmsHost() : null;
        String mdmsPath = userConfiguration != null ? userConfiguration.getMdmsPath() : null;

        if (mdmsEnabled && StringUtils.isNotBlank(mdmsHost) && StringUtils.isNotBlank(mdmsPath)) {
            try {
                allowedTypes = fetchAddressTypesFromMdms(requestInfo, stateTenant, mdmsHost, mdmsPath);
                if (!allowedTypes.isEmpty()) {
                    log.info("[ADDRESS-MDMS] Successfully loaded {} address types from MDMS for tenant: {}",
                            allowedTypes.size(), stateTenant);
                    CacheEntry newEntry = new CacheEntry(allowedTypes, System.currentTimeMillis());
                    tenantAddressTypeCache.put(stateTenant, newEntry);
                    return newEntry.getAllowedTypes();
                }
            } catch (Exception e) {
                log.warn("[ADDRESS-MDMS] Failed to fetch AddressType from MDMS ({}). Falling back to application.properties: {}",
                        e.getClass().getSimpleName(), e.getMessage());
            }
        }

        // Fallback to application.properties
        allowedTypes = parsePropertiesAllowedTypes();
        log.info("[ADDRESS-MDMS] Loaded {} default address types from application.properties", allowedTypes.size());
        CacheEntry newEntry = new CacheEntry(allowedTypes, System.currentTimeMillis());
        tenantAddressTypeCache.put(stateTenant, newEntry);
        return newEntry.getAllowedTypes();
    }

    @Override
    public void validateAddressType(RequestInfo requestInfo, String tenantId, String addressType) {
        if (StringUtils.isBlank(addressType)) {
            log.error("[ADDRESS-MDMS] Validation failed: Address Type is null or blank");
            throw new CustomException(UserConstants.ERROR_MANDATORY_FIELD_MISSING, UserConstants.MSG_ADDRESS_TYPE_MANDATORY);
        }

        String normalizedType = addressType.trim().toUpperCase();
        if ("CORRESPONDANCE".equals(normalizedType)) {
            normalizedType = "CORRESPONDENCE";
        }

        Set<String> allowedTypes = getAllowedAddressTypes(requestInfo, tenantId);

        if (!allowedTypes.contains(normalizedType)) {
            log.error("[ADDRESS-MDMS] Address type '{}' is invalid for tenant '{}'. Allowed types: {}",
                    addressType, tenantId, allowedTypes);
            throw new CustomException(UserConstants.ERROR_INVALID_ADDRESS_TYPE,
                    "Address type '" + addressType + "' is invalid. Allowed types configured in UPYOG MDMS/properties: " + allowedTypes);
        }
    }

    @Override
    public void clearCache() {
        log.info("[ADDRESS-MDMS] Clearing address type cache for all tenants (count: {})", tenantAddressTypeCache.size());
        tenantAddressTypeCache.clear();
    }

    @Override
    public Set<String> refreshCache(RequestInfo requestInfo, String tenantId) {
        String stateTenant = getStateLevelTenant(tenantId);
        log.info("[ADDRESS-MDMS] Manually refreshing address type cache for tenant: {}", stateTenant);
        tenantAddressTypeCache.remove(stateTenant);
        return getAllowedAddressTypes(requestInfo, stateTenant);
    }

    @Override
    public Map<String, Set<String>> getCachedAddressTypes() {
        Map<String, Set<String>> snapshot = new HashMap<>();
        tenantAddressTypeCache.forEach((tenant, entry) -> snapshot.put(tenant, entry.getAllowedTypes()));
        return Collections.unmodifiableMap(snapshot);
    }

    /**
     * Daily scheduled eviction of the address type cache at midnight (or configured cron).
     * Ensures MDMS master data is fetched at most once per 24 hours per tenant.
     */
    @Scheduled(cron = "${address.type.cache.cron}")
    public void scheduledDailyCacheEviction() {
        log.info("[ADDRESS-MDMS] Triggering scheduled daily eviction of AddressType cache. Cleared {} tenant entries.",
                tenantAddressTypeCache.size());
        tenantAddressTypeCache.clear();
    }

    /**
     * Executes the MDMS search query to fetch active AddressType master items.
     *
     * @param requestInfo caller RequestInfo
     * @param stateTenant state-level tenant identifier
     * @param mdmsHost    MDMS host URL
     * @param mdmsPath    MDMS search endpoint path
     * @return set of active uppercase address type codes
     */
    private Set<String> fetchAddressTypesFromMdms(RequestInfo requestInfo, String stateTenant, String mdmsHost, String mdmsPath) {
        String moduleName = (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getMdmsAddressTypeModuleName()))
                ? userConfiguration.getMdmsAddressTypeModuleName()
                : "common-masters";
        String masterName = (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getMdmsAddressTypeMasterName()))
                ? userConfiguration.getMdmsAddressTypeMasterName()
                : "AddressType";
        String filter = (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getMdmsAddressTypeFilter()))
                ? userConfiguration.getMdmsAddressTypeFilter()
                : "[?(@.active==true)]";

        String url = mdmsHost.endsWith("/")
                ? mdmsHost + mdmsPath
                : mdmsHost + "/" + mdmsPath;

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

        log.debug("[ADDRESS-MDMS] Querying MDMS at URL: {} for module: {}, master: {}", url, moduleName, masterName);
        JsonNode responseNode = restTemplate.postForObject(url, criteriaReq, JsonNode.class);

        Set<String> types = new HashSet<>();
        if (responseNode != null) {
            JsonNode mdmsRes = responseNode.path("MdmsRes").path(moduleName).path(masterName);
            if (mdmsRes.isArray()) {
                for (JsonNode item : mdmsRes) {
                    if (item.hasNonNull("code")) {
                        types.add(item.get("code").asText().trim().toUpperCase());
                    }
                }
            }
        }
        return types;
    }

    /**
     * Parses the comma-separated allowed address types from UserConfiguration or defaults.
     *
     * @return set of configured uppercase address type strings
     */
    private Set<String> parsePropertiesAllowedTypes() {
        String allowedProps = userConfiguration != null ? userConfiguration.getAllowedAddressTypes() : null;
        if (StringUtils.isBlank(allowedProps)) {
            return Arrays.stream(UserConstants.DEFAULT_ALLOWED_ADDRESS_TYPES.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
        }
        return Arrays.stream(allowedProps.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * Derives the state-level tenant ID from a given tenant (e.g. "pb.amritsar" -> "pb").
     *
     * @param tenantId input tenant
     * @return state-level tenant string
     */
    private String getStateLevelTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getStateLevelTenantId()))
                    ? userConfiguration.getStateLevelTenantId()
                    : "pb";
        }
        return tenantId.contains(".") ? tenantId.split("\\.")[0] : tenantId;
    }
}
