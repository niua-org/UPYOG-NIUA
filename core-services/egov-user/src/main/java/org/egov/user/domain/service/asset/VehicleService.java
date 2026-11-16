package org.egov.user.domain.service.asset;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.asset.UserVehicle;
import org.egov.user.web.contract.asset.VehicleRequest;
import org.egov.user.web.contract.asset.VehicleSearchCriteria;

import java.util.List;

/**
 * Domain service contract governing standalone CRUD and lifecycle management of motor vehicle assets
 * in the UPYOG user registry.
 *
 * <p>Supports creation, updates, querying with rich criteria, and deactivation (soft-deletion)
 * when citizen members modify their asset declarations.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface VehicleService {

    /**
     * Creates a new vehicle declaration for a citizen.
     *
     * @param request vehicle creation request payload
     * @return persisted {@link UserVehicle} domain entity with populated IDs and audit timestamps
     */
    UserVehicle createVehicle(VehicleRequest request);

    /**
     * Updates an existing vehicle declaration for a citizen.
     *
     * @param request vehicle update request payload
     * @return updated {@link UserVehicle} domain entity
     */
    UserVehicle updateVehicle(VehicleRequest request);

    /**
     * Searches vehicle declarations matching the specified criteria.
     *
     * @param criteria    query criteria filters
     * @param requestInfo standard UPYOG request context envelope
     * @return list of matching {@link UserVehicle} entities
     */
    List<UserVehicle> searchVehicles(VehicleSearchCriteria criteria, RequestInfo requestInfo);

    /**
     * Deactivates / soft-deletes a vehicle declaration by setting its status to {@code INACTIVE}.
     *
     * @param request vehicle delete/deactivation request payload
     * @return deactivated {@link UserVehicle} entity
     */
    UserVehicle deleteVehicle(VehicleRequest request);
}
