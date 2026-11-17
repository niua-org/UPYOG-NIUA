package org.egov.user.web.contract.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;

import java.util.List;

/**
 * Search criteria contract for querying citizen vehicle records from {@code ug_user_vehicle}.
 *
 * <p>Supports both flat and nested payloads matching standard UPYOG criteria conventions.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Search criteria envelope for querying citizen vehicle records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class VehicleSearchCriteria {

    /**
     * Standard UPYOG RequestInfo contract envelope.
     */
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * Nested criteria wrapper for clients wrapping filters inside {@code criteria}.
     */
    @JsonProperty("criteria")
    private VehicleSearchCriteria criteria;

    /**
     * Citizen {@code uuid} owning the vehicle.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * List of citizen UUIDs to filter vehicles across multiple household members.
     */
    @JsonProperty("userUuids")
    private List<String> userUuids;

    /**
     * Unique vehicle primary key ID.
     */
    @JsonProperty("vehicleId")
    private String vehicleId;

    /**
     * Alternate primary key ID alias.
     */
    @JsonProperty("id")
    private String id;

    /**
     * List of vehicle primary key IDs.
     */
    @JsonProperty("vehicleIds")
    private List<String> vehicleIds;

    /**
     * Motor vehicle registration plate number (e.g. DL01AB1234).
     */
    @JsonProperty("vehicleNumber")
    private String vehicleNumber;

    /**
     * Tenant identifier scoping the vehicle record.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Vehicle classification type (e.g. 2_WHEELER, 4_WHEELER, COMMERCIAL).
     */
    @JsonProperty("vehicleType")
    private String vehicleType;

    /**
     * Fuel type of the vehicle (e.g. PETROL, DIESEL, ELECTRIC, CNG, HYBRID).
     */
    @JsonProperty("fuelType")
    private String fuelType;

    /**
     * Vehicle classification category (e.g. 2_WHEELER, 4_WHEELER, COMMERCIAL, LMV, HMV).
     */
    @JsonProperty("category")
    private String category;

    /**
     * Lifecycle status filter: {@code ACTIVE}, {@code INACTIVE}, or {@code ALL}.
     */
    @JsonProperty("status")
    private String status;

    /**
     * Convenience boolean filter for active status (defaults to true if null and status is unspecified).
     */
    @JsonProperty("active")
    private Boolean active;

    /**
     * Pagination offset.
     */
    @JsonProperty("offset")
    private Integer offset;

    /**
     * Pagination limit.
     */
    @JsonProperty("limit")
    private Integer limit;

    /**
     * Resolves the primary citizen {@code userUuid} from available contract properties.
     *
     * @return resolved citizen UUID, or null
     */
    public String resolveUserUuid() {
        if (StringUtils.isNotBlank(userUuid)) {
            return userUuid.trim();
        }
        if (criteria != null && StringUtils.isNotBlank(criteria.getUserUuid())) {
            return criteria.getUserUuid().trim();
        }
        return null;
    }

    /**
     * Resolves the vehicle primary key identifier from available contract properties.
     *
     * @return resolved vehicle ID, or null
     */
    public String resolveVehicleId() {
        if (StringUtils.isNotBlank(vehicleId)) {
            return vehicleId.trim();
        }
        if (StringUtils.isNotBlank(id)) {
            return id.trim();
        }
        if (criteria != null) {
            if (StringUtils.isNotBlank(criteria.getVehicleId())) {
                return criteria.getVehicleId().trim();
            }
            if (StringUtils.isNotBlank(criteria.getId())) {
                return criteria.getId().trim();
            }
        }
        return null;
    }
}
