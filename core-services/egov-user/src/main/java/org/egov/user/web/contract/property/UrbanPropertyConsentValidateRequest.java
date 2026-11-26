package org.egov.user.web.contract.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;

/**
 * Request payload to validate an issued property consent token or active verification session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrbanPropertyConsentValidateRequest {

    /**
     * Standard platform request header containing user authentication and audit metadata.
     */
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * Unique property identifier whose verification status is being validated.
     */
    @NotBlank(message = "propertyId is mandatory")
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * Consent token issued upon successful OTP verification.
     */
    @NotBlank(message = "consentToken is mandatory")
    @JsonProperty("consentToken")
    private String consentToken;
}
