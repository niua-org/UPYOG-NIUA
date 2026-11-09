package org.egov.user.domain.service.config;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.web.contract.config.FamilyConfig;

/**
 * Service interface managing dynamic State Family Configuration.
 *
 * <p>Defines the contract for retrieving active family configuration and validating
 * incoming API operations against the active state policy.</p>
 *
 * <p>Decouples business validation from the data provider source, allowing pluggable
 * implementations (MDMS, Redis, Database) without affecting callers.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyConfigurationService {

    /**
     * Retrieves the currently active family configuration for the given tenant.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param tenantId    target tenant identifier
     * @return the active {@link FamilyConfig} for the tenant
     */
    FamilyConfig getActiveFamilyConfiguration(RequestInfo requestInfo, String tenantId);

    /**
     * Validates that the family concept is supported for the state/tenant.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param tenantId    target tenant identifier
     * @throws org.egov.tracer.model.CustomException if family is not supported
     */
    void validateFamilySupported(RequestInfo requestInfo, String tenantId);

    /**
     * Validates that the tenant supports UPYOG native internal family operations.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param tenantId    target tenant identifier
     * @throws org.egov.tracer.model.CustomException if native family operations are not permitted
     */
    void validateUpyogNativeFamily(RequestInfo requestInfo, String tenantId);

    /**
     * Validates that the tenant supports State Family Registry integration.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param tenantId    target tenant identifier
     * @throws org.egov.tracer.model.CustomException if state family integration is not permitted
     */
    void validateStateRegistryFamily(RequestInfo requestInfo, String tenantId);

    /**
     * Invalidate cached configurations across all tenants.
     */
    void clearCache();

    /**
     * Refreshes the configuration cache for a specific tenant.
     *
     * @param requestInfo caller's RequestInfo envelope
     * @param tenantId    target tenant identifier
     * @return the refreshed {@link FamilyConfig}
     */
    FamilyConfig refreshCache(RequestInfo requestInfo, String tenantId);
}
