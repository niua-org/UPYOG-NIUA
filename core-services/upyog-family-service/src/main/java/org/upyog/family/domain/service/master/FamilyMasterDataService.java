package org.upyog.family.domain.service.master;

import org.egov.common.contract.request.RequestInfo;

import java.util.Set;

/**
 * Service contract responsible for managing and dynamically validating family master data from UPYOG MDMS.
 *
 * <p>Enables runtime addition and modification of relationships and categories in MDMS without requiring service redeployment.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyMasterDataService {

    /**
     * Retrieves the set of valid uppercase relationship codes for the specified tenant from UPYOG MDMS.
     * Caches results in-memory with configurable TTL to optimize network roundtrips.
     *
     * @param requestInfo RequestInfo of the caller
     * @param tenantId    tenant identifier
     * @return set of valid uppercase relationship codes
     */
    Set<String> getAllowedRelationships(RequestInfo requestInfo, String tenantId);

    /**
     * Validates whether the given relationship code is valid under UPYOG MDMS definitions for the tenant.
     *
     * @param requestInfo  RequestInfo of the caller
     * @param tenantId     tenant identifier
     * @param relationship relationship string to validate
     */
    void validateRelationship(RequestInfo requestInfo, String tenantId, String relationship);

    /**
     * Retrieves the set of valid uppercase family category codes for the specified tenant from UPYOG MDMS.
     *
     * @param requestInfo RequestInfo of the caller
     * @param tenantId    tenant identifier
     * @return set of valid uppercase category codes
     */
    Set<String> getAllowedCategories(RequestInfo requestInfo, String tenantId);

    /**
     * Validates whether the given category / caste code is valid under UPYOG MDMS definitions for the tenant.
     *
     * @param requestInfo RequestInfo of the caller
     * @param tenantId    tenant identifier
     * @param category    category or caste string to validate
     */
    void validateCategory(RequestInfo requestInfo, String tenantId, String category);

    /**
     * Retrieves allowed occupation codes from UPYOG MDMS or default fallback.
     *
     * @param requestInfo RequestInfo of the caller
     * @param tenantId    tenant identifier
     * @return set of valid uppercase occupation codes
     */
    Set<String> getAllowedOccupations(RequestInfo requestInfo, String tenantId);

    /**
     * Validates whether the occupation string is valid under MDMS taxonomy for the tenant.
     *
     * @param requestInfo RequestInfo of the caller
     * @param tenantId    tenant identifier
     * @param occupation  occupation string to validate
     */
    void validateOccupation(RequestInfo requestInfo, String tenantId, String occupation);

    /**
     * Retrieves allowed property types from UPYOG PropertyTax MDMS or default fallback.
     *
     * @param requestInfo RequestInfo of the caller
     * @param tenantId    tenant identifier
     * @return set of valid uppercase property types
     */
    Set<String> getAllowedPropertyTypes(RequestInfo requestInfo, String tenantId);

    /**
     * Validates whether the property type is valid under MDMS taxonomy for the tenant.
     *
     * @param requestInfo  RequestInfo of the caller
     * @param tenantId     tenant identifier
     * @param propertyType property type to validate
     */
    void validatePropertyType(RequestInfo requestInfo, String tenantId, String propertyType);

    /**
     * Retrieves allowed property usages from UPYOG PropertyTax MDMS or default fallback.
     *
     * @param requestInfo RequestInfo of the caller
     * @param tenantId    tenant identifier
     * @return set of valid uppercase property usage categories
     */
    Set<String> getAllowedPropertyUsages(RequestInfo requestInfo, String tenantId);

    /**
     * Validates whether the property usage is valid under MDMS taxonomy for the tenant.
     *
     * @param requestInfo   RequestInfo of the caller
     * @param tenantId      tenant identifier
     * @param propertyUsage property usage to validate
     */
    void validatePropertyUsage(RequestInfo requestInfo, String tenantId, String propertyUsage);

    /**
     * Evicts the relationship cache across all tenants.
     */
    void clearCache();

    /**
     * Manually invalidates and refreshes the relationship cache from UPYOG MDMS for a specific tenant.
     *
     * @param requestInfo RequestInfo of the caller
     * @param tenantId    tenant identifier
     * @return refreshed set of valid uppercase relationship codes
     */
    Set<String> refreshCache(RequestInfo requestInfo, String tenantId);
}
