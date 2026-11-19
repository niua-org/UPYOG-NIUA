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
 * Controller response DTO for TAN verification endpoints in requester-services-dx.
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
public class TanVerificationResponse {

    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("isVerified")
    private Boolean isVerified;

    @JsonProperty("maskedTan")
    private String maskedTan;

    @JsonProperty("tanDetails")
    private TanDetails tanDetails;

    @JsonProperty("errorDetails")
    private TanErrorDetails errorDetails;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TanErrorDetails {
        @JsonProperty("errorCode")
        private String errorCode;

        @JsonProperty("errorMessage")
        private String errorMessage;
    }
}
