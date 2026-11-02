package org.egov.dx.adapter.vendor.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Inbound DTO representing a failure response received from the third-party Aadhaar KYC gateway
 * when OTP verification fails (e.g. invalid OTP, expired OTP, maximum attempts exceeded).
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
public class VendorVerifyOtpFailureResponse {

    /**
     * HTTP error code returned by vendor gateway (e.g. 400, 422).
     */
    @JsonProperty("code")
    private Integer code;

    /**
     * Epoch timestamp of error response.
     */
    @JsonProperty("timestamp")
    private Long timestamp;

    /**
     * Unique vendor transaction ID.
     */
    @JsonProperty("transaction_id")
    @JsonAlias({"transactionId", "txnId"})
    private String transactionId;

    /**
     * Nested error details.
     */
    @JsonProperty("data")
    private VendorVerifyOtpFailureResponseDetails data;

    /**
     * Nested details describing the gateway failure reason.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class VendorVerifyOtpFailureResponseDetails {

        /**
         * Gateway entity descriptor.
         */
        @JsonProperty("@entity")
        @JsonAlias("entity")
        private String entity;

        /**
         * Failure reason or error message returned by gateway (e.g., "Invalid OTP", "OTP Expired").
         */
        @JsonProperty("message")
        private String message;
    }
}