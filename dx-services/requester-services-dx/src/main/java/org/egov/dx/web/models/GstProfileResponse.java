package org.egov.dx.web.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.common.contract.response.ResponseInfo;

/**
 * Controller response DTO for GST profile endpoints in requester-services-dx.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GstProfileResponse {

    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("isVerified")
    private Boolean isVerified;

    @JsonProperty("maskedGstin")
    private String maskedGstin;

    /**
     * Source of verification response (e.g., "LIVE_IRIS" or "MOCK").
     */
    @JsonProperty("source")
    private String source;

    /**
     * Visible indicator whether this verification originated from a mock adapter.
     */
    @JsonProperty("isMock")
    private Boolean isMock;

    @JsonProperty("taxpayerDetails")
    private GstTaxpayerDetails taxpayerDetails;

    @JsonProperty("errorDetails")
    private GstErrorDetails errorDetails;

    /**
     * Error details payload when GST verification fails or returns vendor error.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GstErrorDetails {
        /**
         * Machine-readable error code.
         */
        @JsonProperty("errorCode")
        private String errorCode;

        /**
         * Human-readable error description.
         */
        @JsonProperty("errorMessage")
        private String errorMessage;
    }
}
