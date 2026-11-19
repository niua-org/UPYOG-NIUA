package org.egov.dx.util;

/**
 * Constants for GST verification and profile operations in requester-services-dx.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class GstConstants {

    private GstConstants() {
        // Utility class
    }

    // Standard 15-digit GSTIN Regex
    public static final String GSTIN_REGEX = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$";

    // Status strings
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILURE";
    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_INACTIVE = "Inactive";
    public static final String STATUS_CANCELLED = "Cancelled";
    public static final String STATUS_SUSPENDED = "Suspended";

    // Error codes
    public static final String ERR_CODE_INVALID_REQUEST = "INVALID_GST_REQUEST";
    public static final String ERR_CODE_INVALID_FORMAT = "INVALID_GSTIN_FORMAT";
    public static final String ERR_CODE_RECORD_NOT_FOUND = "IRIS-GST-4001";
    public static final String ERR_CODE_GATEWAY_ERROR = "GST_GATEWAY_ERROR";
    public static final String ERR_CODE_TENANT_DISABLED = "VERIFICATION_DISABLED_FOR_TENANT";

    // Error messages
    public static final String MSG_INVALID_REQUEST = "GST profile search request or RequestInfo cannot be null";
    public static final String MSG_BLANK_GSTIN = "GSTIN is mandatory";
    public static final String MSG_INVALID_FORMAT = "Invalid GSTIN format. GSTIN must be a 15-character alphanumeric string (e.g., 03AAAPS0001B1Z5).";
    public static final String MSG_RECORD_NOT_FOUND = "GSTIN does not exist or is inactive in GST records.";
    public static final String MSG_GATEWAY_ERROR = "Unable to process GST search with GST gateway.";
    public static final String MSG_TENANT_DISABLED = "GST profile search service is not enabled for tenant in MDMS configuration.";
}
