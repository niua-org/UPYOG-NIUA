package org.egov.user.web.contract.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.response.ResponseInfo;

/**
 * Response payload returning verified property consent token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrbanPropertyOtpVerifyResponse {

    /**
     * Standard platform response header indicating API execution status.
     */
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    /**
     * Flag indicating whether OTP verification was successful.
     */
    @JsonProperty("verified")
    private Boolean verified;

    /**
     * Cryptographically unique authorization token proving successful OTP consent for linking.
     */
    @JsonProperty("propertyConsentToken")
    private String propertyConsentToken;

    /**
     * Municipal property identifier bound to this verification consent token.
     */
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * Tenant identifier representing the municipality where property is registered.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Epoch timestamp in milliseconds when this consent token will expire.
     */
    @JsonProperty("expiresAt")
    private Long expiresAt;

    /**
     * Descriptive message detailing the result of the OTP verification.
     */
    @JsonProperty("message")
    private String message;
}
