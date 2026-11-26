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

/**
 * Verified TAN entity details returned to UPYOG callers.
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
public class TanDetails {

    /**
     * Unmasked TAN (omitted in responses for PII protection).
     */
    @JsonProperty("tan")
    private String tan;

    @JsonProperty("maskedTan")
    private String maskedTan;

    @JsonProperty("nameOfDeductor")
    private String nameOfDeductor;

    @JsonProperty("categoryOfDeductor")
    private String categoryOfDeductor;

    @JsonProperty("status")
    private String status;

    @JsonProperty("emailId")
    private String emailId;

    /**
     * Boolean indicator whether applicant supplied deductor name matched the TAN record.
     */
    @JsonProperty("nameMatch")
    private Boolean nameMatch;

    @JsonProperty("address")
    private TanAddress address;

    @JsonProperty("jurisdictionDetails")
    private TanJurisdiction jurisdictionDetails;

    /**
     * Postal address details for the deductor entity.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TanAddress {
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
     * Assessing Officer (AO) jurisdiction details for the TAN deductor.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TanJurisdiction {
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
}
