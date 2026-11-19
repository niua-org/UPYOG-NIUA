package org.egov.dx.util;

/**
 * Constants for TAN verification operations in requester-services-dx.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class TanConstants {

    private TanConstants() {
        // Utility class
    }

    public static final String DEFAULT_SERVICE_ID = "10002";
    public static final String DEFAULT_VERSION = "1.0";

    // TAN Format: 4 uppercase letters, 5 digits, 1 uppercase letter
    public static final String TAN_REGEX = "^[A-Z]{4}[0-9]{5}[A-Z]$";

    // Status strings
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILURE";
    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_INACTIVE = "Inactive";

    // Error codes
    public static final String ERR_CODE_INVALID_REQUEST = "INVALID_TAN_REQUEST";
    public static final String ERR_CODE_INVALID_FORMAT = "INVALID_TAN_FORMAT";
    public static final String ERR_CODE_RECORD_NOT_FOUND = "ITD-TAN-4001";
    public static final String ERR_CODE_GATEWAY_ERROR = "TAN_GATEWAY_ERROR";
    public static final String ERR_CODE_TENANT_DISABLED = "VERIFICATION_DISABLED_FOR_TENANT";

    // Error messages
    public static final String MSG_INVALID_REQUEST = "TAN verification request or RequestInfo cannot be null";
    public static final String MSG_BLANK_TAN = "TAN number is mandatory";
    public static final String MSG_INVALID_FORMAT = "Invalid TAN format. TAN must be a 10-character alphanumeric string (e.g., BLRM12345C).";
    public static final String MSG_RECORD_NOT_FOUND = "TAN does not exist or is inactive in Income Tax records.";
    public static final String MSG_GATEWAY_ERROR = "Unable to process TAN verification with Income Tax gateway.";
    public static final String MSG_TENANT_DISABLED = "TAN verification service is not enabled for tenant in MDMS configuration.";
}
