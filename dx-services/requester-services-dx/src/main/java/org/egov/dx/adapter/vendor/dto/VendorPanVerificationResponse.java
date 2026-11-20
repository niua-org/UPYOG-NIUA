package org.egov.dx.adapter.vendor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Inbound DTO representing the response payload returned by the Income Tax Department (ITD)
 * PAN Verification Web Service (Service ID: 10001 External Agency specification).
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
public class VendorPanVerificationResponse {

    @JsonProperty("header")
    private ResponseHeader header;

    @JsonProperty("responseParams")
    private ResponseParams responseParams;

    @JsonProperty("errorDetails")
    private VendorErrorDetails errorDetails;

    /**
     * Response header returned by ITD PAN gateway.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseHeader {
        @JsonProperty("requestId")
        private String requestId;

        @JsonProperty("serviceId")
        private String serviceId;

        @JsonProperty("responseId")
        private String responseId;

        @JsonProperty("status")
        private String status;

        @JsonProperty("statusCode")
        private String statusCode;
    }

    /**
     * Payload parameters returned by ITD on successful PAN search/verification.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseParams {
        @JsonProperty("pan")
        private String pan;

        @JsonProperty("panStatus")
        private String panStatus;

        @JsonProperty("fullName")
        private String fullName;

        @JsonProperty("category")
        private String category;

        @JsonProperty("nameMatch")
        private String nameMatch;

        @JsonProperty("dobMatch")
        private String dobMatch;

        @JsonProperty("dob")
        private String dob;

        @JsonProperty("gender")
        private String gender;

        @JsonProperty("seedingStatus")
        private String seedingStatus;
    }

    /**
     * Error payload returned by ITD when PAN lookup fails.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VendorErrorDetails {
        @JsonProperty("errorCode")
        private String errorCode;

        @JsonProperty("errorMessage")
        private String errorMessage;
    }
}
