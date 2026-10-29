package org.egov.dx.web.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.util.AadhaarConstants;

/**
 * Controller request DTO for Aadhaar OTP-based operations in requester-services-dx.
 *
 * <p>Supports both operations:</p>
 * <ul>
 *   <li><b>Send OTP (/_sendOtp):</b> requires {@code RequestInfo} and a valid 12-digit {@code aadhaarNumber}.</li>
 *   <li><b>Verify OTP (/_verifyOtp):</b> requires {@code RequestInfo}, {@code referenceId} (or {@code txnId}), and 6-digit {@code otp}.</li>
 * </ul>
 *
 * <p>Aadhaar number and OTP are strictly excluded from {@link #toString()} to prevent credential and PII leakage in logs.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"aadhaarNumber", "otp"})
public class AadhaarVerificationRequest {

    /**
     * Standard eGov RequestInfo header containing audit, auth token, and API metadata.
     */
    @NotNull(message = AadhaarConstants.MSG_REQUEST_INFO_MANDATORY)
    @Valid
    @JsonProperty("RequestInfo")
    @JsonAlias("requestInfo")
    private RequestInfo requestInfo;

    /**
     * The 12-digit UIDAI Aadhaar number of the citizen (mandatory for /_sendOtp).
     * Must not start with 0 or 1 per UIDAI specifications.
     */
    @Pattern(regexp = AadhaarConstants.REGEX_AADHAAR, message = AadhaarConstants.MSG_INVALID_AADHAAR)
    @JsonProperty("aadhaarNumber")
    @JsonAlias("aadhaar_number")
    private String aadhaarNumber;

    /**
     * Unique session reference identifier returned by /_sendOtp (mandatory for /_verifyOtp).
     */
    @JsonProperty("referenceId")
    @JsonAlias({"reference_id", "txnId", "transactionId"})
    private String referenceId;

    /**
     * 6-digit one-time password entered by citizen (mandatory for /_verifyOtp).
     */
    @Pattern(regexp = AadhaarConstants.REGEX_OTP, message = AadhaarConstants.MSG_INVALID_OTP)
    @JsonProperty("otp")
    private String otp;

    /**
     * Optional flag indicating if this verification is triggered to re-sync an existing citizen profile.
     */
    @JsonProperty("isResync")
    @JsonAlias({"resync", "is_resync"})
    private Boolean isResync;

    /**
     * Optional user UUID correlation identifier when re-syncing an existing citizen profile.
     */
    @JsonProperty("userUuid")
    @JsonAlias({"uuid", "user_uuid"})
    private String userUuid;

    /**
     * Convenience alias for {@link #referenceId} to maintain compatibility with legacy txnId references.
     *
     * @return the referenceId / transactionId
     */
    public String getTxnId() {
        return referenceId;
    }

    /**
     * Convenience alias to set {@link #referenceId}.
     *
     * @param txnId the transaction ID / reference ID
     */
    public void setTxnId(String txnId) {
        this.referenceId = txnId;
    }

    /**
     * Returns masked Aadhaar representation (e.g. XXXXXXXX1234) for secure logging.
     *
     * @return masked Aadhaar string
     */
    public String getMaskedAadhaar() {
        if (aadhaarNumber == null || aadhaarNumber.length() < 4) {
            return AadhaarConstants.DEFAULT_MASKED_AADHAAR;
        }
        return aadhaarNumber.replaceAll(AadhaarConstants.REGEX_AADHAAR_MASK, AadhaarConstants.MASK_CHAR);
    }
}