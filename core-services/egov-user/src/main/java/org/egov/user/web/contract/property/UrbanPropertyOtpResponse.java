package org.egov.user.web.contract.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.response.ResponseInfo;

/**
 * Response payload confirming OTP dispatch to property owner.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrbanPropertyOtpResponse {

    /**
     * Standard platform response header indicating API execution status.
     */
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    /**
     * Unique session reference ID generated for tracking this OTP challenge.
     */
    @JsonProperty("referenceId")
    private String referenceId;

    /**
     * Municipal property identifier for which the OTP was generated.
     */
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * Masked mobile number of the property owner who was sent the OTP.
     */
    @JsonProperty("maskedMobileNumber")
    private String maskedMobileNumber;

    /**
     * Descriptive message indicating the outcome of the OTP request.
     */
    @JsonProperty("message")
    private String message;
}
