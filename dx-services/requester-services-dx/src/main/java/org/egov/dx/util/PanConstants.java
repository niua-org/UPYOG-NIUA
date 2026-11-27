package org.egov.dx.util;

/**
 * Constants for PAN verification operations in requester-services-dx.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class PanConstants {

    private PanConstants() {
        // Utility class
    }

    public static final String DEFAULT_SERVICE_ID = "10001";
    public static final String DEFAULT_VERSION = "1.0";

    // PAN Format: 5 uppercase letters, 4 digits, 1 uppercase letter
    public static final String PAN_REGEX = "^[A-Z]{5}[0-9]{4}[A-Z]$";

    // Status strings
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILURE";
    public static final String STATUS_EXISTING_AND_VALID = "Existing and Valid";
    public static final String STATUS_INOPERATIVE = "Inoperative";
    public static final String STATUS_INVALID = "Invalid";
    public static final String STATUS_DELETED = "Deleted";

    // Verification source identifiers
    public static final String SOURCE_MOCK = "MOCK";
    public static final String SOURCE_LIVE_ITD = "LIVE_ITD";

    // Matching flags
    public static final String MATCH_YES = "Y";
    public static final String MATCH_NO = "N";

    // Error codes
    public static final String ERR_CODE_INVALID_REQUEST = "INVALID_PAN_REQUEST";
    public static final String ERR_CODE_INVALID_FORMAT = "INVALID_PAN_FORMAT";
    public static final String ERR_CODE_MISSING_NAME = "MISSING_PAN_NAME";
    public static final String ERR_CODE_MISSING_DOB = "MISSING_PAN_DOB";
    public static final String ERR_CODE_RECORD_NOT_FOUND = "ITD-PAN-4001";
    public static final String ERR_CODE_GATEWAY_ERROR = "PAN_GATEWAY_ERROR";
    public static final String ERR_CODE_TENANT_DISABLED = "VERIFICATION_DISABLED_FOR_TENANT";
    public static final String ERR_CODE_UNAUTHORIZED_USER = "UNAUTHORIZED_USER";

    // Error messages
    public static final String MSG_INVALID_REQUEST = "PAN verification request or RequestInfo cannot be null";
    public static final String MSG_BLANK_PAN = "PAN number is mandatory";
    public static final String MSG_INVALID_FORMAT = "Invalid PAN format. PAN must be a 10-character alphanumeric string (e.g., ABCDE1234F).";
    public static final String MSG_MISSING_NAME = "Full name as per PAN is mandatory for verification.";
    public static final String MSG_MISSING_DOB = "Date of birth (dob) is mandatory for PAN verification.";
    public static final String MSG_RECORD_NOT_FOUND = "PAN does not exist or is inactive in Income Tax records.";
    public static final String MSG_GATEWAY_ERROR = "Unable to process PAN verification with Income Tax gateway.";
    public static final String MSG_TENANT_DISABLED = "PAN verification service is not enabled for tenant in MDMS configuration.";
    public static final String MSG_UNAUTHORIZED_USER = "Authenticated user information (userInfo with uuid) is required to access verification services.";
}
