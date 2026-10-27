package org.egov.dx.web.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.common.contract.response.ResponseInfo;

/**
 * Controller response DTO for Aadhaar verification endpoints in requester-services-dx.
 *
 * <p>Returned by both:</p>
 * <ul>
 *   <li><b>/_sendOtp:</b> returns {@code status} ("OTP_SENT"), {@code referenceId}, and {@code txnId}.</li>
 *   <li><b>/_verifyOtp:</b> returns {@code isVerified} (true/false), {@code maskedAadhaar}, and {@link UserDemographics} e-KYC profile.</li>
 * </ul>
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
public class AadhaarVerificationResponse {

    /**
     * Standard eGov ResponseInfo block containing request matching metadata and response status.
     */
    @JsonProperty("ResponseInfo")
    @JsonAlias("responseInfo")
    private ResponseInfo responseInfo;

    /**
     * Vendor transaction identifier.
     */
    @JsonProperty("txnId")
    private String txnId;

    /**
     * Unique session reference ID needed to verify the OTP.
     */
    @JsonProperty("referenceId")
    private String referenceId;

    /**
     * High-level operation status (e.g., "OTP_SENT", "VERIFIED", "INVALID_OTP", "FAILED").
     */
    @JsonProperty("status")
    private String status;

    /**
     * Informational message describing the result (e.g., "OTP sent successfully", "Aadhaar verified successfully").
     */
    @JsonProperty("message")
    private String message;

    /**
     * Masked registered mobile number (e.g., "XXXXXX9012") if returned by provider.
     */
    @JsonProperty("maskedMobile")
    private String maskedMobile;

    /**
     * Boolean indicator stating whether Aadhaar verification succeeded.
     */
    @JsonProperty("isVerified")
    private Boolean isVerified;

    /**
     * Masked Aadhaar number (e.g., "XXXXXXXX1234").
     */
    @JsonProperty("maskedAadhaar")
    private String maskedAadhaar;

    /**
     * Authenticated citizen demographics and residential address obtained from UIDAI e-KYC.
     */
    @JsonProperty("demographics")
    private UserDemographics demographics;
}