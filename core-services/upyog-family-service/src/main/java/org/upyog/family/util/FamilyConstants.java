package org.upyog.family.util;

/**
 * Common constants, error codes, and validation messages for UPYOG Family Service.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class FamilyConstants {

    private FamilyConstants() {
        // Prevent instantiation
    }

    public static final String ERROR_INVALID_REQUEST = "INVALID_REQUEST";
    public static final String ERROR_FAMILY_NOT_FOUND = "FAMILY_NOT_FOUND";
    public static final String ERROR_HEAD_OF_FAMILY_REQUIRED = "HEAD_OF_FAMILY_REQUIRED";
    public static final String ERROR_DUPLICATE_HEAD = "DUPLICATE_HEAD_OF_FAMILY";
    public static final String ERROR_HEAD_UUID_MISMATCH = "HEAD_UUID_MISMATCH";
    public static final String ERROR_MEMBER_ALREADY_EXISTS = "MEMBER_ALREADY_EXISTS_IN_ACTIVE_FAMILY";
    public static final String ERROR_USER_NOT_FOUND = "CITIZEN_NOT_FOUND";
    public static final String ERROR_FILESTORE_NOT_FOUND = "FILESTORE_NOT_FOUND";
    public static final String ERROR_INVALID_CATEGORY = "INVALID_FAMILY_CATEGORY";
    public static final String ERROR_INVALID_RELATIONSHIP = "INVALID_RELATIONSHIP";
    public static final String ERROR_INVALID_SPLIT = "INVALID_FAMILY_SPLIT";
    public static final String ERROR_INVALID_MERGE = "INVALID_FAMILY_MERGE";

    // Specialized Member & HoF Action Error Codes
    public static final String ERROR_CONSENT_INVALID = "INVALID_AADHAAR_CONSENT";
    public static final String ERROR_MEMBER_NOT_FOUND = "FAMILY_MEMBER_NOT_FOUND";
    public static final String ERROR_HEAD_CANNOT_LEAVE = "HEAD_CANNOT_LEAVE_WITHOUT_TRANSFER";
    public static final String ERROR_INVALID_HOF_ASSIGNMENT = "INVALID_HOF_ASSIGNMENT";
    public static final String ERROR_ALREADY_HEAD = "MEMBER_ALREADY_HEAD_OF_FAMILY";
    public static final String ERROR_MEMBER_INACTIVE = "FAMILY_MEMBER_ALREADY_INACTIVE";

    public static final String MSG_HEAD_OF_FAMILY_MANDATORY = "A family unit must have exactly one Head of Family (HoF) with is_head=true";
    public static final String MSG_FAMILY_TENANT_REQUIRED = "Tenant ID is mandatory for family operations";
    public static final String MSG_FAMILY_ID_REQUIRED = "Family ID is mandatory";
}
