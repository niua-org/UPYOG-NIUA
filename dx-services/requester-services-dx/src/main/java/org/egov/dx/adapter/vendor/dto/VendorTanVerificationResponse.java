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
 * TAN Verification Web Service (v1.0 External Agency specification).
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
public class VendorTanVerificationResponse {

    @JsonProperty("header")
    private ResponseHeader header;

    @JsonProperty("tanDetails")
    private VendorTanDetails tanDetails;

    @JsonProperty("errorDetails")
    private VendorErrorDetails errorDetails;

    /**
     * Response header returned by ITD TAN gateway.
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

        @JsonProperty("responseId")
        private String responseId;

        @JsonProperty("status")
        private String status;

        @JsonProperty("statusCode")
        private String statusCode;

        @JsonProperty("timestamp")
        private String timestamp;
    }

    /**
     * Entity details returned by ITD for the queried TAN.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VendorTanDetails {
        @JsonProperty("tan")
        private String tan;

        @JsonProperty("nameOfDeductor")
        private String nameOfDeductor;

        @JsonProperty("categoryOfDeductor")
        private String categoryOfDeductor;

        @JsonProperty("status")
        private String status;

        @JsonProperty("emailId")
        private String emailId;

        @JsonProperty("address")
        private VendorAddress address;

        @JsonProperty("jurisdictionDetails")
        private VendorJurisdiction jurisdictionDetails;
    }

    /**
     * Address information for the deductor entity.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VendorAddress {
        @JsonProperty("line1")
        private String line1;

        @JsonProperty("line2")
        private String line2;

        @JsonProperty("city")
        private String city;

        @JsonProperty("state")
        private String state;

        @JsonProperty("pinCode")
        private String pinCode;
    }

    /**
     * Jurisdictional AO details for the deductor entity.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VendorJurisdiction {
        @JsonProperty("aoCode")
        private String aoCode;

        @JsonProperty("aoType")
        private String aoType;

        @JsonProperty("rangeCode")
        private String rangeCode;

        @JsonProperty("aoNumber")
        private String aoNumber;

        @JsonProperty("description")
        private String description;
    }

    /**
     * Error payload returned by ITD when TAN lookup fails.
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
