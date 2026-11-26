package org.egov.user.web.contract.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.domain.model.asset.UserVehicle;

import java.util.Collections;
import java.util.List;

/**
 * Standard UPYOG response contract for vehicle CRUD and lifecycle operations.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Response envelope for vehicle mutations and search queries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class VehicleResponse {

    /**
     * Standard UPYOG ResponseInfo contract header.
     */
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    /**
     * List of retrieved or mutated vehicle domain entities.
     */
    @JsonProperty("vehicles")
    @Builder.Default
    private List<UserVehicle> vehicles = Collections.emptyList();

    /**
     * Total count of vehicle records returned.
     */
    @JsonProperty("totalCount")
    private Integer totalCount;

    /**
     * Convenience constructor wrapping a single vehicle domain entity.
     *
     * @param responseInfo response status metadata
     * @param vehicle      the created, updated, or deactivated vehicle
     */
    public VehicleResponse(ResponseInfo responseInfo, UserVehicle vehicle) {
        this.responseInfo = responseInfo;
        this.vehicles = vehicle != null ? Collections.singletonList(vehicle) : Collections.emptyList();
        this.totalCount = this.vehicles.size();
    }
}
