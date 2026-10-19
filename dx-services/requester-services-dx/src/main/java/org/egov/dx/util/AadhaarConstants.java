package org.egov.dx.util;

/**
 * Constants for Aadhaar Verification and OTP Operations in requester-services-dx.
 *
 * <p>Centralizes all string literals, validation regex patterns, status codes,
 * error codes, default messages, third-party gateway entity identifiers, and HTTP header names.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class AadhaarConstants {

    private AadhaarConstants() {
        // Prevent instantiation
    }

    /* -------------------------------------------------------------------------
     * Validation Regular Expressions
     * ---------------------------------------------------------------------- */
    public static final String REGEX_AADHAAR = "^[2-9]{1}[0-9]{11}$";
    public static final String REGEX_OTP = "^[0-9]{6}$";
    public static final String REGEX_AADHAAR_MASK = ".(?=.{4})";

    /* -------------------------------------------------------------------------
     * Request & Validation Error Messages
     * ---------------------------------------------------------------------- */
    public static final String MSG_REQUEST_BODY_NULL = "Request body must not be null";
    public static final String MSG_REQUEST_INFO_MANDATORY = "RequestInfo header is mandatory";
    public static final String MSG_INVALID_AADHAAR = "Aadhaar number must be a valid 12-digit numeric identifier starting with 2-9";
    public static final String MSG_MISSING_REFERENCE_ID = "referenceId is mandatory for OTP verification";
    public static final String MSG_INVALID_OTP = "OTP must be exactly 6 numeric digits";
    public static final String MSG_CONSENT_MANDATORY = "Citizen consent is mandatory";
    public static final String MSG_REF_ID_MANDATORY = "Reference ID is mandatory for OTP verification";

    /* -------------------------------------------------------------------------
     * Gateway & Operation Status Codes
     * ---------------------------------------------------------------------- */
    public static final String STATUS_OTP_SENT = "OTP_SENT";
    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_INVALID_OTP = "INVALID_OTP";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_VALID = "VALID";

    /* -------------------------------------------------------------------------
     * Error Codes
     * ---------------------------------------------------------------------- */
    public static final String ERR_CODE_INVALID_REQUEST = "INVALID_REQUEST";
    public static final String ERR_CODE_INVALID_REQUEST_INFO = "INVALID_REQUEST_INFO";
    public static final String ERR_CODE_INVALID_AADHAAR = "INVALID_AADHAAR";
    public static final String ERR_CODE_INVALID_AADHAAR_CHECKSUM = "INVALID_AADHAAR_CHECKSUM";
    public static final String ERR_CODE_GATEWAY_ERROR = "GATEWAY_ERROR";
    public static final String ERR_CODE_MISSING_REFERENCE_ID = "MISSING_REFERENCE_ID";
    public static final String ERR_CODE_INVALID_OTP_FORMAT = "INVALID_OTP_FORMAT";
    public static final String ERR_CODE_MAX_OTP_ATTEMPTS_EXCEEDED = "MAX_OTP_ATTEMPTS_EXCEEDED";
    public static final String ERR_CODE_OTP_ALREADY_VERIFIED = "OTP_ALREADY_VERIFIED";

    /* -------------------------------------------------------------------------
     * Operation Success & Informational Messages
     * ---------------------------------------------------------------------- */
    public static final String MSG_OTP_SENT_SUCCESS = "OTP sent successfully to registered mobile number";
    public static final String MSG_VERIFY_SUCCESS = "Aadhaar verified successfully";
    public static final String MSG_INVALID_OR_EXPIRED_OTP = "Invalid or expired OTP";
    public static final String MSG_GATEWAY_DISPATCH_FAILED = "Failed to dispatch OTP from Aadhaar gateway";
    public static final String MSG_GATEWAY_NO_RESPONSE = "Aadhaar gateway verification returned no response";
    public static final String MSG_MAX_OTP_ATTEMPTS_EXCEEDED = "Maximum invalid OTP attempts exceeded. Transaction reference ID is locked. Please request a new OTP.";
    public static final String MSG_OTP_ALREADY_VERIFIED = "This OTP transaction has already been verified and consumed. Replay is not permitted.";
    public static final String MSG_INVALID_AADHAAR_CHECKSUM = "Aadhaar number failed UIDAI Verhoeff checksum validation.";

    /* -------------------------------------------------------------------------
     * Third-Party Gateway / Sandbox Entity Identifiers & HTTP Headers
     * ---------------------------------------------------------------------- */
    public static final String ENTITY_SEND_OTP_REQUEST = "in.co.sandbox.kyc.aadhaar.okyc.otp.request";
    public static final String ENTITY_SEND_OTP_RESPONSE = "in.co.sandbox.kyc.aadhaar.okyc.otp.response";
    public static final String ENTITY_VERIFY_OTP_REQUEST = "in.co.sandbox.kyc.aadhaar.okyc.request";
    public static final String ENTITY_VERIFY_OTP_RESPONSE = "in.co.sandbox.kyc.aadhaar.okyc.response";

    public static final String HEADER_API_KEY = "x-api-key";
    public static final String HEADER_API_SECRET = "x-api-secret";
    public static final String HEADER_API_VERSION = "x-api-version";

    /* -------------------------------------------------------------------------
     * Defaults & Security Constants
     * ---------------------------------------------------------------------- */
    public static final String DEFAULT_CONSENT = "Y";
    public static final String DEFAULT_REASON = "For UPYOG Citizen Profile verification";
    public static final String DEFAULT_MASKED_AADHAAR = "XXXX";
    public static final String MASK_CHAR = "X";
}
