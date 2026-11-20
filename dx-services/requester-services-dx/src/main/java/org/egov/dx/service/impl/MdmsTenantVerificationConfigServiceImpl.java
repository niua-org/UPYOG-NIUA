package org.egov.dx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.service.TenantVerificationConfigService;
import org.egov.dx.util.Configurations;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MDMS-backed implementation of {@link TenantVerificationConfigService}.
 *
 * <p>Strictly satisfies SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles querying MDMS for tenant verification enablement.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Injects Spring {@link RestTemplate} and configuration abstractions.</li>
 * </ul>
 *
 * <p>Features built-in in-memory caching with TTL and ULB tenant lookup with automatic state tenant fallback.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class MdmsTenantVerificationConfigServiceImpl implements TenantVerificationConfigService {

    private final RestTemplate restTemplate;
    private final Configurations configurations;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, GenderCacheEntry> genderCache = new ConcurrentHashMap<>();

    private record CacheEntry(boolean enabled, long timestamp) {}
    private record GenderCacheEntry(Set<String> genders, long timestamp) {}

    /**
     * Constructor injection satisfying Dependency Inversion Principle (DIP).
     *
     * @param restTemplate standard Spring RestTemplate bean
     * @param configurations centralized application configurations
     */
    @Autowired
    public MdmsTenantVerificationConfigServiceImpl(RestTemplate restTemplate,
                                                   Configurations configurations) {
        this.restTemplate = restTemplate;
        this.configurations = configurations;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether the specified verification service (e.g. TAN, PAN, GST) is enabled in MDMS.
     * Caches results with a configurable TTL (default: 300s). For ULB tenants (e.g. {@code pg.citya}),
     * first inspects ULB-level configuration; if not defined, seamlessly falls back to the state tenant
     * (e.g. {@code pg}). Fails closed (returns false) on any missing configuration or MDMS connection error.</p>
     *
     * @param requestInfo standard eGov RequestInfo header
     * @param tenantId municipal tenant identifier (e.g., "pg.citya" or "pg")
     * @param serviceType verification service category ("TAN", "PAN", "GST")
     * @return true if enabled, false otherwise or on error
     */
    @Override
    public boolean isVerificationEnabled(RequestInfo requestInfo, String tenantId, String serviceType) {
        // Guard check: fail closed if either tenant identifier or verification service category is unspecified
        if (StringUtils.isBlank(tenantId) || StringUtils.isBlank(serviceType)) {
            log.warn("[MDMS-VERIFICATION-CHECK] Blank tenantId or serviceType provided (tenantId={}, serviceType={})",
                    tenantId, serviceType);
            return false;
        }

        String cacheKey = tenantId.trim().toLowerCase() + ":" + serviceType.trim().toUpperCase();
        long ttlMs = configurations.getMdmsVerificationCacheTtlSeconds() * 1000L;

        // Caching optimization (Review comment Medium #3): Avoid repeated downstream MDMS HTTP calls for static master data
        CacheEntry cached = cache.get(cacheKey);
        // Expiry check: serve from memory if cache entry exists and is within the configured TTL window
        if (cached != null && (System.currentTimeMillis() - cached.timestamp()) < ttlMs) {
            log.debug("[MDMS-VERIFICATION-CHECK] Cache hit for key {}: enabled={}", cacheKey, cached.enabled());
            return cached.enabled();
        }

        String host = configurations.getMdmsHost();
        String endpoint = configurations.getMdmsSearchEndpoint();
        // Configuration guard: fail safely if MDMS service location is unconfigured in properties
        if (StringUtils.isBlank(host) || StringUtils.isBlank(endpoint)) {
            log.error("[MDMS-VERIFICATION-CHECK] egov.mdms.host or egov.mdms.search.endpoint is not configured");
            return false;
        }

        String moduleName = configurations.getMdmsVerificationModule();
        String masterName = configurations.getMdmsVerificationMaster();
        String url = buildMdmsUrl(host, endpoint);

        // Step 1: Query MDMS using specific requested tenantId (e.g., city/ULB tenant "pg.citya")
        Optional<Boolean> enabled = queryMdms(requestInfo, url, tenantId, moduleName, masterName, serviceType);

        // Step 2: Tenant fallback hierarchy (Review comment Medium #3): If ULB tenant has no explicit master record,
        // fall back to state-level configuration (e.g. "pg") so municipal tenants inherit state-wide defaults
        if (enabled.isEmpty() && tenantId.contains(".")) {
            String stateTenant = tenantId.split("\\.")[0];
            log.debug("[MDMS-VERIFICATION-CHECK] Falling back to state tenant {} for service {}", stateTenant, serviceType);
            enabled = queryMdms(requestInfo, url, stateTenant, moduleName, masterName, serviceType);
        }

        boolean finalEnabled = enabled.orElse(false);
        // Warning log: record when no configuration was located at either ULB or state tier
        if (enabled.isEmpty()) {
            log.warn("[MDMS-VERIFICATION-CHECK] No MDMS configuration found for service {} and tenant {}", serviceType, tenantId);
        }

        // Store result in cache with current timestamp to throttle subsequent queries
        cache.put(cacheKey, new CacheEntry(finalEnabled, System.currentTimeMillis()));
        return finalEnabled;
    }

    private Optional<Boolean> queryMdms(RequestInfo requestInfo, String url, String tenantId,
                                        String moduleName, String masterName, String serviceType) {
        MdmsCriteriaReq criteriaReq = buildCriteriaReq(requestInfo, tenantId, moduleName, masterName);
        try {
            log.debug("[MDMS-VERIFICATION-CHECK] Calling MDMS at {} for tenant {} and service {}", url, tenantId, serviceType);
            JsonNode responseNode = restTemplate.postForObject(url, criteriaReq, JsonNode.class);
            return isServiceEnabledInResponse(responseNode, moduleName, masterName, serviceType, tenantId);
        } catch (Exception e) {
            log.error("[MDMS-VERIFICATION-CHECK] Failed to query MDMS for tenant verification check (tenant={}, service={}): {}",
                    tenantId, serviceType, e.getMessage(), e);
            return Optional.of(false);
        }
    }

    private String buildMdmsUrl(String host, String endpoint) {
        return host.endsWith("/") ? host + endpoint : host + "/" + endpoint;
    }

    private MdmsCriteriaReq buildCriteriaReq(RequestInfo requestInfo, String targetTenant, String moduleName, String masterName) {
        return buildCriteriaReq(requestInfo, targetTenant, moduleName, masterName, null);
    }

    private MdmsCriteriaReq buildCriteriaReq(RequestInfo requestInfo, String targetTenant, String moduleName,
                                            String masterName, String filter) {
        MasterDetail.MasterDetailBuilder masterDetailBuilder = MasterDetail.builder().name(masterName);
        // Optional filter expression (e.g. "[?(@.active==true)]")
        if (StringUtils.isNotBlank(filter)) {
            masterDetailBuilder.filter(filter);
        }
        ModuleDetail moduleDetail = ModuleDetail.builder()
                .moduleName(moduleName)
                .masterDetails(Collections.singletonList(masterDetailBuilder.build()))
                .build();
        MdmsCriteria criteria = new MdmsCriteria();
        criteria.setTenantId(targetTenant);
        criteria.setModuleDetails(Collections.singletonList(moduleDetail));

        MdmsCriteriaReq criteriaReq = new MdmsCriteriaReq();
        criteriaReq.setRequestInfo(requestInfo != null ? requestInfo : new RequestInfo());
        criteriaReq.setMdmsCriteria(criteria);
        return criteriaReq;
    }

    private Optional<Boolean> isServiceEnabledInResponse(JsonNode responseNode, String moduleName, String masterName,
                                                         String serviceType, String tenantId) {
        // Defensive check: return empty if downstream response payload was unparseable or empty
        if (responseNode == null) {
            return Optional.empty();
        }
        JsonNode configs = responseNode.path("MdmsRes").path(moduleName).path(masterName);
        // Master record validation: ensure master node is a non-empty JSON array
        if (!configs.isArray() || configs.isEmpty()) {
            return Optional.empty();
        }
        for (JsonNode config : configs) {
            // Flexible schema support: check 'type' or legacy 'service' field name
            String type = config.hasNonNull("type")
                    ? config.path("type").asText()
                    : config.path("service").asText();
            // Case-insensitive match on verification category (e.g. "PAN", "TAN", "GST")
            if (serviceType.equalsIgnoreCase(type)) {
                boolean enabled = config.path("enabled").asBoolean(false);
                log.info("[MDMS-VERIFICATION-CHECK] Verification service {} is {} in MDMS for tenant {}",
                        serviceType, enabled ? "ENABLED" : "DISABLED", tenantId);
                return Optional.of(enabled);
            }
        }
        // No match found in the master array for this service type
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Queries MDMS {@code common-masters.GenderType} for active gender definitions for the given tenant
     * with state fallback and in-memory TTL caching. Normalizes codes to uppercase and populates standard
     * single-letter and full representations for ITD PAN verification matching.</p>
     *
     * @param requestInfo standard eGov RequestInfo header
     * @param tenantId municipal tenant identifier (e.g., "pg.citya" or "pg")
     * @return set of valid gender strings
     */
    @Override
    public Set<String> getValidGenders(RequestInfo requestInfo, String tenantId) {
        // Guard check: fail closed if tenantId is unspecified
        if (StringUtils.isBlank(tenantId)) {
            log.warn("[MDMS-GENDER-FETCH] Blank tenantId provided for gender lookup");
            return Collections.emptySet();
        }

        String cacheKey = tenantId.trim().toLowerCase();
        long ttlMs = configurations.getMdmsVerificationCacheTtlSeconds() * 1000L;

        // Caching check: avoid repeated MDMS lookups for static gender master data
        GenderCacheEntry cached = genderCache.get(cacheKey);
        // Expiry check: return cached gender set if still within TTL
        if (cached != null && (System.currentTimeMillis() - cached.timestamp()) < ttlMs) {
            log.debug("[MDMS-GENDER-FETCH] Cache hit for tenant {}: {}", cacheKey, cached.genders());
            return cached.genders();
        }

        String host = configurations.getMdmsHost();
        String endpoint = configurations.getMdmsSearchEndpoint();
        // Configuration check: warn if MDMS service coordinates are missing
        if (StringUtils.isBlank(host) || StringUtils.isBlank(endpoint)) {
            log.error("[MDMS-GENDER-FETCH] egov.mdms.host or egov.mdms.search.endpoint is not configured");
            return Collections.emptySet();
        }

        String moduleName = configurations.getMdmsGenderModule();
        String masterName = configurations.getMdmsGenderMaster();
        String url = buildMdmsUrl(host, endpoint);

        // First attempt with requested tenant
        Set<String> genders = queryGendersFromMdms(requestInfo, url, tenantId, moduleName, masterName);

        // State tenant fallback: If ULB tenant returns empty set, query state tenant (e.g., "pg")
        if ((genders == null || genders.isEmpty()) && tenantId.contains(".")) {
            String stateTenant = tenantId.split("\\.")[0];
            log.debug("[MDMS-GENDER-FETCH] Falling back to state tenant {} for gender master", stateTenant);
            genders = queryGendersFromMdms(requestInfo, url, stateTenant, moduleName, masterName);
        }

        Set<String> finalGenders = (genders != null && !genders.isEmpty())
                ? Collections.unmodifiableSet(genders)
                : Collections.emptySet();

        // Store result in cache
        genderCache.put(cacheKey, new GenderCacheEntry(finalGenders, System.currentTimeMillis()));
        return finalGenders;
    }

    private Set<String> queryGendersFromMdms(RequestInfo requestInfo, String url, String tenantId,
                                            String moduleName, String masterName) {
        MdmsCriteriaReq criteriaReq = buildCriteriaReq(requestInfo, tenantId, moduleName, masterName, "[?(@.active==true)]");

        try {
            log.debug("[MDMS-GENDER-FETCH] Calling MDMS at {} for tenant {} and master {}.{}",
                    url, tenantId, moduleName, masterName);
            JsonNode responseNode = restTemplate.postForObject(url, criteriaReq, JsonNode.class);
            return parseGendersFromResponse(responseNode, moduleName, masterName);
        } catch (Exception e) {
            log.error("[MDMS-GENDER-FETCH] Failed to query MDMS for gender master (tenant={}): {}",
                    tenantId, e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    private Set<String> parseGendersFromResponse(JsonNode responseNode, String moduleName, String masterName) {
        Set<String> validGenders = new HashSet<>();
        // Guard check: return empty set if response is null
        if (responseNode == null) {
            return validGenders;
        }

        JsonNode masterNodes = responseNode.path("MdmsRes").path(moduleName).path(masterName);
        // Ensure master node exists and is an array of items
        if (masterNodes.isArray()) {
            for (JsonNode item : masterNodes) {
                String code = item.path("code").asText(null);
                // Ensure code is non-blank
                if (StringUtils.isNotBlank(code)) {
                    validGenders.add(code.trim().toUpperCase());
                }
            }
        }
        return validGenders;
    }
}
