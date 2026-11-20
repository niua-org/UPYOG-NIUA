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
 * Verified GST taxpayer details returned to UPYOG callers.
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
public class GstTaxpayerDetails {

    @JsonProperty("gstin")
    private String gstin;

    @JsonProperty("maskedGstin")
    private String maskedGstin;

    @JsonProperty("legalName")
    private String legalName;

    @JsonProperty("tradeName")
    private String tradeName;

    @JsonProperty("pan")
    private String pan;

    @JsonProperty("taxpayerType")
    private String taxpayerType;

    @JsonProperty("constitutionOfBusiness")
    private String constitutionOfBusiness;

    @JsonProperty("status")
    private String status;

    @JsonProperty("registrationDate")
    private String registrationDate;

    @JsonProperty("cancellationDate")
    private String cancellationDate;

    @JsonProperty("stateCode")
    private String stateCode;

    @JsonProperty("state")
    private String state;

    @JsonProperty("principalPlaceOfBusiness")
    private GstAddress principalPlaceOfBusiness;

    /**
     * Physical address representation for taxpayer's principal place of business.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GstAddress {
        @JsonProperty("doorNumber")
        private String doorNumber;

        @JsonProperty("buildingName")
        private String buildingName;

        @JsonProperty("street")
        private String street;

        @JsonProperty("location")
        private String location;

        @JsonProperty("city")
        private String city;

        @JsonProperty("district")
        private String district;

        @JsonProperty("state")
        private String state;

        @JsonProperty("pincode")
        private String pincode;

        @JsonProperty("fullAddress")
        private String fullAddress;
    }
}
