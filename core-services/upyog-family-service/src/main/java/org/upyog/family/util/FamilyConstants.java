package org.upyog.family.util;

/**
 * Common constants, domain values, error codes, and validation messages for UPYOG Family Service.
 * Centralized constant class serving as the single source of truth across all modules.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class FamilyConstants {

    private FamilyConstants() {
        // Prevent instantiation
    }

    // ==========================================
    // System & Audit
    // ==========================================
    public static final String SYSTEM_USER = "SYSTEM";

    // ==========================================
    // Family & Member Statuses
    // ==========================================
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_EXITED = "EXITED";
    public static final String STATUS_SPLIT = "SPLIT";
    public static final String STATUS_MERGED = "MERGED";
    public static final String STATUS_STAGED = "STAGED";
    public static final String STATUS_FOUND = "FOUND";
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String STATUS_READY_FOR_JIT = "READY_FOR_JIT_PROVISION";

    // ==========================================
    // Member Relationships with HoF
    // ==========================================
    public static final String RELATIONSHIP_HEAD = "HEAD";
    public static final String RELATIONSHIP_SPOUSE = "SPOUSE";
    public static final String RELATIONSHIP_SON = "SON";
    public static final String RELATIONSHIP_DAUGHTER = "DAUGHTER";
    public static final String RELATIONSHIP_CHILD = "CHILD";
    public static final String RELATIONSHIP_FATHER = "FATHER";
    public static final String RELATIONSHIP_MOTHER = "MOTHER";
    public static final String RELATIONSHIP_BROTHER = "BROTHER";
    public static final String RELATIONSHIP_SISTER = "SISTER";
    public static final String RELATIONSHIP_GRANDFATHER = "GRANDFATHER";
    public static final String RELATIONSHIP_GRANDMOTHER = "GRANDMOTHER";
    public static final String RELATIONSHIP_GRANDSON = "GRANDSON";
    public static final String RELATIONSHIP_GRANDDAUGHTER = "GRANDDAUGHTER";
    public static final String RELATIONSHIP_DAUGHTER_IN_LAW = "DAUGHTER_IN_LAW";
    public static final String RELATIONSHIP_SON_IN_LAW = "SON_IN_LAW";
    public static final String RELATIONSHIP_DEPENDENT = "DEPENDENT";
    public static final String RELATIONSHIP_GUARDIAN = "GUARDIAN";
    public static final String RELATIONSHIP_OTHER = "OTHER";

    // ==========================================
    // Family Categories
    // ==========================================
    public static final String CATEGORY_GENERAL = "GENERAL";
    public static final String CATEGORY_OBC = "OBC";
    public static final String CATEGORY_SC = "SC";
    public static final String CATEGORY_ST = "ST";
    public static final String CATEGORY_EWS = "EWS";

    // ==========================================
    // Member Exit Reasons
    // ==========================================
    public static final String EXIT_REASON_MARRIAGE = "MARRIAGE";
    public static final String EXIT_REASON_RELOCATION = "RELOCATION";
    public static final String EXIT_REASON_DEMISE = "DEMISE";
    public static final String EXIT_REASON_OTHER = "OTHER";

    // ==========================================
    // MDMS Modules & Masters
    // ==========================================
    public static final String MDMS_MODULE_COMMON_MASTERS = "common-masters";
    public static final String MDMS_MASTER_FAMILY_RELATIONSHIP = "FamilyRelationship";
    public static final String MDMS_MASTER_FAMILY_CATEGORY = "FamilyCategory";

    // ==========================================
    // Prefixes & Sources
    // ==========================================
    public static final String PREFIX_FAM = "FAM-";
    public static final String PREFIX_MEM = "MEM-";
    public static final String PREFIX_REF = "REF-";
    public static final String PREFIX_EXT = "EXT_";
    public static final String SOURCE_STATE_REGISTRY_WRAPPER = "STATE_REGISTRY_WRAPPER";

    // ==========================================
    // Error Codes
    // ==========================================
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
    public static final String ERROR_UNAUTHORIZED_ACCESS = "UNAUTHORIZED_ACCESS";

    // State Registry Integration Error Codes
    public static final String ERROR_INVALID_STATE_FAMILY_REQUEST = "INVALID_STATE_FAMILY_REQUEST";
    public static final String ERROR_INVALID_STATE_FAMILY_ID = "INVALID_STATE_FAMILY_ID";
    public static final String ERROR_INVALID_TENANT_ID = "INVALID_TENANT_ID";
    public static final String ERROR_INVALID_MEMBERS_LIST = "INVALID_MEMBERS_LIST";
    public static final String ERROR_STATE_FAMILY_NOT_FOUND = "STATE_FAMILY_NOT_FOUND";

    // ==========================================
    // Validation & Error Messages
    // ==========================================
    public static final String MSG_HEAD_OF_FAMILY_MANDATORY = "A family unit must have exactly one Head of Family (HoF) with is_head=true";
    public static final String MSG_FAMILY_TENANT_REQUIRED = "Tenant ID is mandatory for family operations";
    public static final String MSG_FAMILY_ID_REQUIRED = "Family ID is mandatory";
}
