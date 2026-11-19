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
 * TAN Verification Web Service (v1.0 External Agency specification).
 *
 * <p>Excludes raw {@code tan} from {@link #toString()} to adhere to UPYOG secure logging policies.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"searchCriteria"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorTanVerificationRequest {

    @JsonProperty("header")
    private RequestHeader header;

    @JsonProperty("searchCriteria")
    private SearchCriteria searchCriteria;

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

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString(exclude = {"tan"})
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchCriteria {
        @JsonProperty("tan")
        private String tan;
    }
}
