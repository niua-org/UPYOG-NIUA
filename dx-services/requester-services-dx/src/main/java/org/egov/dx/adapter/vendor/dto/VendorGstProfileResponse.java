package org.egov.dx.adapter.vendor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Inbound DTO representing the response returned by the IRIS Peridot / Taxpayer GST Search API.
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
public class VendorGstProfileResponse {

    @JsonProperty("status_cd")
    private String statusCode;

    @JsonProperty("status_desc")
    private String statusDesc;

    @JsonProperty("data")
    private TaxpayerData data;

    @JsonProperty("error")
    private VendorError error;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaxpayerData {
        @JsonProperty("gstin")
        private String gstin;

        @JsonProperty("lgnm")
        private String legalName;

        @JsonProperty("tradeNam")
        private String tradeName;

        @JsonProperty("ctb")
        private String constitutionOfBusiness;

        @JsonProperty("dty")
        private String taxpayerType;

        @JsonProperty("sts")
        private String status;

        @JsonProperty("rgdt")
        private String registrationDate;

        @JsonProperty("cxdt")
        private String cancellationDate;

        @JsonProperty("pradr")
        private PlaceOfBusiness principalPlaceOfBusiness;

        @JsonProperty("adadr")
        private List<PlaceOfBusiness> additionalPlacesOfBusiness;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaceOfBusiness {
        @JsonProperty("addr")
        private Address address;

        @JsonProperty("ntr")
        private String natureOfBusiness;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        @JsonProperty("bno")
        private String doorNumber;

        @JsonProperty("bnm")
        private String buildingName;

        @JsonProperty("st")
        private String street;

        @JsonProperty("loc")
        private String location;

        @JsonProperty("city")
        private String city;

        @JsonProperty("dst")
        private String district;

        @JsonProperty("stcd")
        private String state;

        @JsonProperty("pncd")
        private String pincode;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VendorError {
        @JsonProperty("error_cd")
        private String errorCode;

        @JsonProperty("message")
        private String message;
    }
}
