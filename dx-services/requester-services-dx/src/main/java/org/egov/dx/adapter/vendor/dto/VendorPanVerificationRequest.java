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
 * Outbound DTO representing the request payload sent to the Income Tax Department (ITD)
 * PAN Verification Web Service (Service ID: 10001 External Agency specification).
 *
 * <p>Excludes raw {@code pan} from {@link #toString()} to prevent PII exposure in logs.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"searchParams"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorPanVerificationRequest {

    @JsonProperty("header")
    private RequestHeader header;

    @JsonProperty("searchParams")
    private SearchParams searchParams;

    /**
     * Standard request header required by ITD PAN external agency gateway.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RequestHeader {
        @JsonProperty("requestId")
        private String requestId;

        @JsonProperty("serviceId")
        private String serviceId;

        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("version")
        private String version;
    }

    /**
     * Demographic search parameters sent to ITD for match validation.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString(exclude = {"pan"})
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchParams {
        @JsonProperty("pan")
        private String pan;

        @JsonProperty("fullName")
        private String fullName;

        @JsonProperty("dob")
        private String dob;

        @JsonProperty("gender")
        private String gender;
    }
}
