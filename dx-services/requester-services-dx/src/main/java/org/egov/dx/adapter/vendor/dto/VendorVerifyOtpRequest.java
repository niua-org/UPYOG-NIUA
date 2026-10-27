package org.egov.dx.adapter.vendor.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.dx.util.AadhaarConstants;

/**
 * Outbound DTO representing the request payload sent to the third-party Aadhaar KYC gateway
 * to verify the citizen-provided 6-digit OTP against an active OTP session reference.
 *
 * <p>OTP is strictly excluded from {@link #toString()} to prevent credential leakage in application logs.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"otp"})
public class VendorVerifyOtpRequest {

    /**
     * Unique reference identifier obtained from the Send OTP response.
     */
    @NotNull(message = AadhaarConstants.MSG_REF_ID_MANDATORY)
    @JsonProperty("reference_id")
    @JsonAlias({"referenceId", "txnId"})
    private Long referenceId;

    /**
     * 6-digit OTP entered by the citizen.
     */
    @NotNull(message = AadhaarConstants.MSG_INVALID_OTP)
    @Pattern(regexp = AadhaarConstants.REGEX_OTP, message = AadhaarConstants.MSG_INVALID_OTP)
    @JsonProperty("otp")
    private String otp;

    /**
     * Entity identifier required by the gateway provider.
     * Example: "in.co.sandbox.kyc.aadhaar.okyc.request"
     */
    @JsonProperty("@entity")
    @Builder.Default
    private String entity = AadhaarConstants.ENTITY_VERIFY_OTP_REQUEST;
}