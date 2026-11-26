package org.egov.user.domain.service.asset;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.web.contract.asset.RuralPropertyRequest;
import org.egov.user.web.contract.asset.RuralPropertySearchCriteria;

import java.util.List;

/**
 * Domain service contract governing standalone CRUD and lifecycle management of rural property assets
 * in UPYOG user registry.
 *
 * <p>Supports creation, updates, querying with rich criteria, and deactivation (soft-deletion)
 * when citizen members modify their asset declarations.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface RuralPropertyService {

    /**
     * Creates a new rural property declaration for a citizen.
     *
     * @param request rural property creation request payload
     * @return persisted {@link UserProperty} domain entity with populated IDs and audit timestamps
     */
    UserProperty createRuralProperty(RuralPropertyRequest request);

    /**
     * Updates an existing rural property declaration for a citizen.
     *
     * @param request rural property update request payload
     * @return updated {@link UserProperty} domain entity
     */
    UserProperty updateRuralProperty(RuralPropertyRequest request);

    /**
     * Searches rural property assets matching the specified criteria.
     *
     * @param criteria    query criteria filters
     * @param requestInfo standard UPYOG request context envelope
     * @return list of matching {@link UserProperty} entities
     */
    List<UserProperty> searchRuralProperties(RuralPropertySearchCriteria criteria, RequestInfo requestInfo);

    /**
     * Deactivates / soft-deletes a rural property by setting its status to {@code INACTIVE}.
     *
     * @param request rural property delete/deactivation request payload
     * @return deactivated {@link UserProperty} entity with {@code INACTIVE} status
     */
    UserProperty deleteRuralProperty(RuralPropertyRequest request);

    /**
     * Direct deactivation helper method marking a rural property as {@code INACTIVE}.
     *
     * @param propertyId  property record primary key ID
     * @param userUuid    citizen user UUID
     * @param requestInfo standard UPYOG request context envelope
     * @return deactivated {@link UserProperty} entity
     */
    UserProperty deactivateRuralProperty(String propertyId, String userUuid, RequestInfo requestInfo);
}
