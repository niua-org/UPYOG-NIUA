package org.egov.dx.adapter.vendor.dto;

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
 * (e.g., Sandbox India OKYC / UIDAI OTP API) to initiate OTP delivery to the citizen's Aadhaar-linked mobile.
 *
 * <p>Aadhaar number is strictly excluded from {@link #toString()} to prevent accidental logging of sensitive PII.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"aadhaarNumber"})
public class VendorSendOtpRequest {

    /**
     * Entity identifier required by the gateway provider.
     * Example: "in.co.sandbox.kyc.aadhaar.okyc.otp.request"
     */
    @JsonProperty("@entity")
    @Builder.Default
    private String entity = AadhaarConstants.ENTITY_SEND_OTP_REQUEST;

    /**
     * The 12-digit UIDAI Aadhaar number of the citizen.
     * Must start with 2-9 and be exactly 12 numeric digits.
     */
    @NotNull(message = AadhaarConstants.MSG_INVALID_AADHAAR)
    @Pattern(regexp = AadhaarConstants.REGEX_AADHAAR, message = AadhaarConstants.MSG_INVALID_AADHAAR)
    @JsonProperty("aadhaar_number")
    private String aadhaarNumber;

    /**
     * Explicit citizen consent indicator ('Y' / 'N') required by UIDAI guidelines.
     */
    @NotNull(message = AadhaarConstants.MSG_CONSENT_MANDATORY)
    @JsonProperty("consent")
    @Builder.Default
    private String consent = AadhaarConstants.DEFAULT_CONSENT;

    /**
     * Business reason/purpose for initiating Aadhaar OTP verification.
     */
    @JsonProperty("reason")
    @Builder.Default
    private String reason = AadhaarConstants.DEFAULT_REASON;

    /**
     * Helper method to return the masked Aadhaar number (e.g. XXXXXXXX1234) for secure logging.
     *
     * @return 8 X's followed by the last 4 digits, or "XXXX" if null/invalid
     */
    public String getMaskedAadhaar() {
        if (aadhaarNumber == null || aadhaarNumber.length() < 4) {
            return AadhaarConstants.DEFAULT_MASKED_AADHAAR;
        }
        return aadhaarNumber.replaceAll(AadhaarConstants.REGEX_AADHAAR_MASK, AadhaarConstants.MASK_CHAR);
    }
}