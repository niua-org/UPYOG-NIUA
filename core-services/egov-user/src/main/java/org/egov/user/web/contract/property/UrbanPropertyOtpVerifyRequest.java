package org.egov.user.web.contract.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;

/**
 * Request payload for verifying an urban property OTP.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrbanPropertyOtpVerifyRequest {

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
     * Unique municipal property identifier being verified.
     */
    @NotBlank(message = "propertyId is mandatory")
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * Reference identifier received in the OTP send response.
     */
    @NotBlank(message = "referenceId is mandatory")
    @JsonProperty("referenceId")
    private String referenceId;

    /**
     * OTP digits entered by the user.
     */
    @NotBlank(message = "otp is mandatory")
    @JsonProperty("otp")
    private String otp;
}
