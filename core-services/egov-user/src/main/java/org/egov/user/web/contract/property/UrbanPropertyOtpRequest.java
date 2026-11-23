package org.egov.user.web.contract.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;

/**
 * Request payload for dispatching an OTP to an urban property's assigned owner mobile number.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrbanPropertyOtpRequest {

    /**
     * Standard platform request header containing user authentication and audit metadata.
     */
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * Tenant identifier representing the urban local body / municipality.
     */
    @NotBlank(message = "tenantId is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Unique property identifier to be linked with the citizen profile.
     */
    @NotBlank(message = "propertyId is mandatory")
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * Optional for single-owner properties (auto-selected).
     * Required if multi-owner property to designate which unlinked owner receives the OTP.
     */
    @JsonProperty("ownerMobileNumber")
    private String ownerMobileNumber;
}
