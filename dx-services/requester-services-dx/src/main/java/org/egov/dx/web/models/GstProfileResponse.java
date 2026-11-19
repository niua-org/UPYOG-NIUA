package org.egov.dx.web.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class GstProfileResponse {

    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("isVerified")
    private Boolean isVerified;

    @JsonProperty("maskedGstin")
    private String maskedGstin;

    @JsonProperty("taxpayerDetails")
    private GstTaxpayerDetails taxpayerDetails;

    @JsonProperty("errorDetails")
    private GstErrorDetails errorDetails;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GstErrorDetails {
        @JsonProperty("errorCode")
        private String errorCode;

        @JsonProperty("errorMessage")
        private String errorMessage;
    }
}
