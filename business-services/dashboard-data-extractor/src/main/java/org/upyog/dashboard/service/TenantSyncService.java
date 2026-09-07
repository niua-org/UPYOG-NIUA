package org.upyog.dashboard.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.upyog.dashboard.common.constants.Module;
import org.upyog.dashboard.constants.DashboardExtractorConstants;
import org.upyog.dashboard.config.DashboardProperties;
import org.upyog.dashboard.config.SchemaMappingConfig;
import org.upyog.dashboard.mdms.client.MdmsClient;
import org.upyog.dashboard.model.IngestionModuleDetail;
import org.upyog.dashboard.repository.IngestionSummaryRepository;
import org.upyog.dashboard.util.CommonUtils;

/**
 * Service responsible for synchronizing tenant lists from MDMS into the local
 * {@code ingestion_module_detail} table and providing cached tenant lookups.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSyncService {

    private final MdmsClient mdmsClient;
    private final IngestionSummaryRepository summaryRepository;
    private final SchemaMappingConfig schemaMappingConfig;
    private final DashboardProperties dashboardProperties;

    /**
     * Synchronizes city tenants from MDMS for the given state and upserts
     * records into {@code ingestion_module_detail} for all enabled modules.
     *
     * @param stateTenantId state identifier (e.g. {@code "pg"})
     * @return list of synced {@link IngestionModuleDetail} records
     */
    @CacheEvict(value = {DashboardExtractorConstants.CACHE_ACTIVE_TENANTS, DashboardExtractorConstants.CACHE_TENANT_MODULE_DETAILS}, allEntries = true)
    public List<IngestionModuleDetail> syncTenantsFromMdms(String stateTenantId) {
        String effectiveState = StringUtils.isNotBlank(stateTenantId) ? stateTenantId
                : dashboardProperties.getMetricState();
        log.info("Starting tenant sync from MDMS for state: {}", effectiveState);

        List<String> cityTenants = mdmsClient.fetchCityTenants(effectiveState);
        if (cityTenants.isEmpty()) {
            log.warn("No city tenants returned from MDMS for state {}. Sync aborted.", effectiveState);
            return Collections.emptyList();
        }

        List<Module> enabledModules = schemaMappingConfig.getEnabledModules();
        if (enabledModules.isEmpty()) {
            log.warn("No modules enabled in SchemaMappingConfig. Defaulting to all known modules.");
        }

        List<IngestionModuleDetail> detailsToUpsert = new ArrayList<>();
        long now = CommonUtils.getCurrentEpochMillis();

        for (String tenantCode : cityTenants) {
            for (Module module : enabledModules) {
                String deterministicId = UUID.nameUUIDFromBytes((tenantCode + ":" + module.name()).getBytes()).toString();
                IngestionModuleDetail detail = IngestionModuleDetail.builder()
                        .detailId(deterministicId)
                        .tenantId(tenantCode)
                        .moduleName(module.name())
                        .active(true)
                        .createdBy(DashboardExtractorConstants.MDMS_SYNC_USER)
                        .createdTime(now)
                        .lastModifiedBy(DashboardExtractorConstants.MDMS_SYNC_USER)
                        .lastModifiedTime(now)
                        .build();
                detailsToUpsert.add(detail);
            }
        }

        summaryRepository.replaceAllModuleDetails(detailsToUpsert);
        log.info("Successfully synced and replaced {} module detail records for {} ULBs",
                detailsToUpsert.size(), cityTenants.size());

        return detailsToUpsert;
    }

    /**
     * Retrieves active tenant IDs for a given module, utilizing in-memory
     * cache. If the database contains no active records, falls back to the
     * configured default tenant.
     *
     * @param module the target module
     * @return list of active tenant IDs
     */
    @Cacheable(value = DashboardExtractorConstants.CACHE_ACTIVE_TENANTS, key = "#module != null ? #module.name() : 'ALL'")
    public List<String> getActiveTenants(Module module) {
        List<String> tenants = (module != null)
                ? summaryRepository.findActiveTenantsByModule(module.name())
                : summaryRepository.findAllActiveTenants();

        if (tenants.isEmpty()) {
            log.warn("No active tenants found in ingestion_module_detail for module {}. Falling back to configured tenantId: {}",
                    module, dashboardProperties.getTenantId());
            if (StringUtils.isNotBlank(dashboardProperties.getTenantId())) {
                return List.of(dashboardProperties.getTenantId());
            }
        }
        return tenants;
    }

    /**
     * Retrieves all active module detail records, utilizing in-memory cache.
     *
     * @return list of active {@link IngestionModuleDetail} entities
     */
    @Cacheable(value = DashboardExtractorConstants.CACHE_TENANT_MODULE_DETAILS)
    public List<IngestionModuleDetail> getActiveModuleDetails() {
        return summaryRepository.findAllActiveModuleDetails();
    }
}
