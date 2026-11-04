package org.egov.user.domain.service.address;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;

import java.util.Map;
import java.util.Set;

/**
 * Service contract responsible for managing and validating Address Master Data according to UPYOG standards.
 *
 * <p>In adherence to the UPYOG and DIGIT architecture, business master data is primarily fetched
 * from the MDMS service (Master Data Management Service, module 'common-masters', master 'AddressType')
 * with graceful fallback to configurations defined in {@code application.properties} ({@code address.types.allowed}).</p>
 *
 * <p>Employs daily caching to minimize network overhead against MDMS, along with manual eviction/refresh
 * capabilities for operations and administration.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface AddressMasterDataService {

    /**
     * Retrieves the set of valid address type codes for the specified tenant, querying MDMS
     * if enabled with fallback to {@code application.properties}. Caches results for daily reuse.
     *
     * @param requestInfo RequestInfo of the caller
     * @param tenantId    tenant identifier
     * @return set of valid uppercase address type codes
     */
    Set<String> getAllowedAddressTypes(RequestInfo requestInfo, String tenantId);

    /**
     * Validates whether the given address type code is permitted under the configured UPYOG MDMS
     * or application properties master definitions.
     *
     * @param requestInfo RequestInfo of the caller
     * @param tenantId    tenant identifier
     * @param addressType address type string to validate
     * @throws CustomException if the address type is null, blank, or not configured
     */
    void validateAddressType(RequestInfo requestInfo, String tenantId, String addressType);

    /**
     * Evicts the address type cache for all tenants.
     */
    void clearCache();

    /**
     * Manually invalidates and refreshes the address type cache from MDMS or application.properties
     * for a specific tenant.
     *
     * @param requestInfo RequestInfo of the caller
     * @param tenantId    tenant identifier
     * @return refreshed set of valid uppercase address type codes
     */
    Set<String> refreshCache(RequestInfo requestInfo, String tenantId);

    /**
     * Returns a snapshot view of currently cached address types grouped by state tenant.
     *
     * @return map of state tenant to set of cached address types
     */
    Map<String, Set<String>> getCachedAddressTypes();
}
