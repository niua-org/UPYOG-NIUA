package org.egov.dx.service;

import org.egov.common.contract.request.RequestInfo;

/**
 * Service contract to verify whether a specific verification capability (TAN, PAN, GST)
 * is enabled for a given tenant in the Master Data Management Service (MDMS).
 *
 * <p>Enforces tenant-level governance before invoking third-party or mock identity verification gateways.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface TenantVerificationConfigService {

    /**
     * Checks whether the verification feature is enabled in MDMS for the specified tenant.
     *
     * @param requestInfo standard RequestInfo
     * @param tenantId the tenant ID (e.g., "pg.citya" or "pg")
     * @param serviceType the verification service type ("TAN", "PAN", "GST")
     * @return true if enabled for this tenant, false otherwise
     */
    boolean isVerificationEnabled(RequestInfo requestInfo, String tenantId, String serviceType);
}
