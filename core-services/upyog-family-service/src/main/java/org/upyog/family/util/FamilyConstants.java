package org.upyog.family.util;

/**
 * Common constants, domain values, error codes, and validation messages for UPYOG Family Service.
 * Centralized constant class serving as the single source of truth across all modules.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class FamilyConstants {

    /**
     * Private constructor to prevent instantiation of utility constants class.
     */
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
    public static final String STATUS_JOINED = "JOINED";
    public static final String STATUS_READY_FOR_JIT = "READY_FOR_JIT_PROVISION";

    // ==========================================
    // Family Types
    // ==========================================
    public static final String TYPE_UPYOG_INTERNAL = "UPYOG_INTERNAL";
    public static final String TYPE_STATE_REGISTRY = "STATE_REGISTRY";

    // ==========================================
    // Member Relationships with HoF
    // ==========================================
    public static final String RELATIONSHIP_SELF = "SELF";
    /**
     * @deprecated In accordance with MDMS taxonomy (#356), {@code HEAD} relationship is deactivated
     *             and {@code SELF} is used for Head of Family. Retained for backwards compatibility.
     */
    @Deprecated
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
    public static final String MDMS_MASTER_FAMILY_CONFIGURATION = "FamilyConfiguration";
    public static final String MDMS_MASTER_OCCUPATION = "Occupation";
    public static final String MDMS_MODULE_PROPERTY_TAX = "PropertyTax";
    public static final String MDMS_MASTER_PROPERTY_TYPE = "PropertyType";
    public static final String MDMS_MASTER_USAGE_CATEGORY = "UsageCategory";

    // ==========================================
    // Prefixes & Sources
    // ==========================================
    public static final String PREFIX_FAM = "FAM-";
    public static final String PREFIX_MEM = "MEM-";
    public static final String PREFIX_REF = "REF-";
    public static final String PREFIX_EXT = "EXT_";
    public static final String SOURCE_CITIZEN_PORTAL = "CITIZEN_PORTAL";
    public static final String SOURCE_CSC = "CSC";
    public static final String SOURCE_STATE_REGISTRY_WRAPPER = "STATE_REGISTRY_WRAPPER";

    // ==========================================
    // Error Codes
    // ==========================================
    public static final String ERROR_INVALID_REQUEST = "INVALID_REQUEST";
    public static final String ERROR_FAMILY_NOT_SUPPORTED = "FAMILY_NOT_SUPPORTED";
    public static final String ERROR_OPERATION_NOT_ALLOWED = "OPERATION_NOT_ALLOWED";
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
    public static final String ERROR_USER_NOT_VERIFIED = "USER_NOT_VERIFIED";

    // State Registry Integration Error Codes
    public static final String ERROR_INVALID_STATE_FAMILY_REQUEST = "INVALID_STATE_FAMILY_REQUEST";
    public static final String ERROR_INVALID_STATE_FAMILY_ID = "INVALID_STATE_FAMILY_ID";
    public static final String ERROR_INVALID_TENANT_ID = "INVALID_TENANT_ID";
    public static final String ERROR_INVALID_MEMBERS_LIST = "INVALID_MEMBERS_LIST";
    public static final String ERROR_STATE_FAMILY_NOT_FOUND = "STATE_FAMILY_NOT_FOUND";

    // ==========================================
    // Locality Types & Default Values
    // ==========================================
    public static final String LOCALITY_TYPE_URBAN = "URBAN";
    public static final String LOCALITY_TYPE_RURAL = "RURAL";
    public static final String CONSENT_TOKEN_PREFIX = "CONSENT-VERIFIED-";
    public static final String DEFAULT_CITIZEN_NAME = "Citizen";

    // ==========================================
    // JSON Keys & Query Parameters
    // ==========================================
    public static final String JSON_KEY_USER = "user";
    public static final String JSON_KEY_REQUEST_INFO = "RequestInfo";
    public static final String JSON_KEY_MOBILE_NUMBER = "mobileNumber";
    public static final String JSON_KEY_UUID = "uuid";
    public static final String JSON_KEY_NAME = "name";
    public static final String JSON_KEY_USER_NAME = "userName";
    public static final String JSON_KEY_GENDER = "gender";
    public static final String PARAM_TENANT_ID = "tenantId";
    public static final String JSON_KEY_REFERENCE_ID = "referenceId";
    public static final String JSON_KEY_OTP = "otp";
    public static final String JSON_KEY_TXN_ID = "txnId";
    public static final String JSON_KEY_IS_VERIFIED = "isVerified";
    public static final String JSON_KEY_AADHAAR_NUMBER = "aadhaarNumber";
    public static final String JSON_KEY_MASKED_AADHAAR = "maskedAadhaar";
    public static final String JSON_KEY_TYPE = "type";
    public static final String JSON_KEY_ACTIVE = "active";
    public static final String JSON_KEY_USER_UUID = "userUuid";
    public static final String JSON_KEY_FAMILY_ID = "familyId";
    public static final String JSON_KEY_CASTE = "caste";
    public static final String JSON_KEY_IS_SPECIALLY_ABLED = "isSpeciallyAbled";
    public static final String JSON_KEY_DOCUMENTS = "documents";
    public static final String JSON_KEY_PROPERTIES = "properties";
    public static final String JSON_KEY_INCOME = "income";
    public static final String JSON_KEY_VEHICLES = "vehicles";
    public static final String JSON_KEY_MEMBERS = "members";
    public static final String JSON_KEY_DOB = "dob";
    public static final String JSON_KEY_IS_IDENTITY_VERIFIED = "isIdentityVerified";
    public static final String JSON_KEY_PHOTO = "photo";
    public static final String JSON_KEY_EMAIL_ID = "emailId";
    public static final String JSON_KEY_STATUS = "status";
    public static final String JSON_KEY_FATHER_NAME = "fatherName";
    public static final String JSON_KEY_MOTHER_NAME = "motherName";
    public static final String JSON_KEY_SPOUSE_NAME = "spouseName";
    public static final String JSON_KEY_GUARDIAN = "guardian";
    public static final String JSON_KEY_GUARDIAN_RELATION = "guardianRelation";
    public static final String JSON_KEY_ALTERNATE_MOBILE_NUMBER = "alternateMobileNumber";

    public static final String USER_TYPE_CITIZEN = "CITIZEN";
    public static final String USER_TYPE_EMPLOYEE = "EMPLOYEE";
    public static final String GENDER_OTHERS = "OTHERS";
    public static final String STATUS_OK = "OK";
    public static final String DEFAULT_MEMBER_ASSET_SYNC_PATH = "/user/profile/_sync-member-assets";

    // ==========================================
    // Validation & Error Messages
    // ==========================================
    public static final String MSG_HEAD_OF_FAMILY_MANDATORY = "A family unit must have exactly one Head of Family (HoF) with is_head=true";
    public static final String MSG_FAMILY_TENANT_REQUIRED = "Tenant ID is mandatory for family operations";
    public static final String MSG_FAMILY_ID_REQUIRED = "Family ID is mandatory";
    public static final String MSG_USER_NOT_VERIFIED = "User identity is not verified in database. Citizen must verify their profile (via Aadhaar/e-KYC) before performing family operations.";
    public static final String MSG_FAMILY_BODY_NULL = "Family request body cannot be null";
    public static final String MSG_HEAD_UUID_MISMATCH = "family.headUserUuid must match the userUuid of the member where isHead is true";
    public static final String MSG_MEMBER_USER_UUID_REQUIRED = "Every family member must have a valid userUuid";
    public static final String MSG_DOCUMENT_FILESTORE_ID_MANDATORY = "Document filestoreId is mandatory";
    public static final String MSG_LOCALITY_TYPE_MANDATORY = "Property localityType is mandatory (URBAN or RURAL)";
    public static final String MSG_RURAL_PROPERTY_DETAIL_REQUIRED = "ruralPropertyDetail is required for RURAL property";
    public static final String MSG_RURAL_GRAM_PANCHAYAT_VILLAGE_MANDATORY = "gramPanchayat and villageName are mandatory for rural property";
    public static final String MSG_URBAN_PROPERTY_DETAIL_REQUIRED = "urbanPropertyDetail is required for URBAN property";
    public static final String MSG_UPYOG_PROPERTY_ID_MANDATORY = "upyogPropertyId is mandatory for urban property";
    public static final String MSG_INVALID_LOCALITY_TYPE_PREFIX = "Invalid localityType: ";
    public static final String MSG_INVALID_LOCALITY_TYPE_SUFFIX = ". Must be URBAN or RURAL";
    public static final String MSG_VEHICLE_NUMBER_MANDATORY = "vehicleNumber is mandatory for declared vehicle";
    public static final String MSG_TARGET_FAMILY_NOT_EXISTS = "Target family does not exist";
    public static final String MSG_PARENT_FAMILY_NOT_ACTIVE = "Parent family is not in ACTIVE state";
    public static final String MSG_MERGE_FAMILY_INTO_ITSELF = "Cannot merge a family unit into itself";
    public static final String MSG_SURVIVING_FAMILY_NOT_ACTIVE = "Surviving family is not ACTIVE";
    public static final String MSG_MERGING_FAMILY_NOT_ACTIVE = "Merging family is not ACTIVE";
    public static final String MSG_FAMILY_NOT_FOUND_PREFIX = "Family not found: ";
    public static final String MSG_PARENT_FAMILY_NOT_FOUND_PREFIX = "Parent family not found: ";
    public static final String MSG_USER_HOST_NOT_CONFIGURED = "egov.user.host is not configured";
    public static final String MSG_USER_SEARCH_PATH_NOT_CONFIGURED = "egov.user.search.path is not configured";
    public static final String MSG_USER_CREATE_PATH_NOT_CONFIGURED = "egov.user.create.path is not configured";
    public static final String MSG_FAMILY_CONFIGURATION_REQUIRED = "FamilyConfiguration is required";
    public static final String MSG_REQUESTER_DX_HOST_NOT_CONFIGURED = "requester.services.dx.host is not configured";
    public static final String MSG_REQUESTER_DX_VERIFY_PATH_NOT_CONFIGURED = "requester.services.dx.verify.otp.path is not configured";
    public static final String MSG_CALLER_INFO_REQUIRED_SEND_OTP = "Authenticated caller information is required to send family OTP.";
    public static final String MSG_CALLER_INFO_REQUIRED_VERIFY_OTP = "Authenticated caller information is required to verify family OTP.";
    public static final String MSG_OTP_REFERENCE_ID_MANDATORY = "referenceId is mandatory for OTP verification.";
    public static final String MSG_INVALID_EXPIRED_OTP = "Invalid or expired OTP entered.";
    public static final String MSG_HOF_RECORD_NOT_ASSOCIATED_PREFIX = "Head of family record not associated with family: ";
    public static final String MSG_HOF_MOBILE_NOT_FOUND = "Mobile number for Head of Family not found.";
    public static final String MSG_FAMILY_ID_PREFIX = "Family with ID ";
    public static final String MSG_NOT_FOUND_SUFFIX = " not found.";
    public static final String MSG_CANNOT_SEND_OTP_INACTIVE_FAMILY_PREFIX = "Cannot send OTP for inactive or closed family: ";
    public static final String MSG_CANNOT_VERIFY_OTP_INACTIVE_FAMILY_PREFIX = "Cannot verify OTP for inactive family: ";
    public static final String MSG_DUPLICATE_MEMBER_UUID_PREFIX = "Duplicate member userUuid found in request: ";
    public static final String MSG_CITIZEN_ALREADY_ACTIVE_PREFIX = "Citizen profile ";
    public static final String MSG_CITIZEN_ALREADY_ACTIVE_SUFFIX = " is already an active member of family: ";
    public static final String MSG_FILESTORE_NOT_FOUND_PREFIX = "FileStore ID does not exist or is inaccessible: ";
    public static final String MSG_CANNOT_UPDATE_FAMILY_STATUS_PREFIX = "Cannot update family with status: ";
    public static final String MSG_ONLY_HOF_AUTHORIZED_UPDATE = "Only the Head of Family is authorized to update family metadata.";
    public static final String MSG_ONLY_PARENT_HOF_AUTHORIZED_SPLIT = "Only the parent family Head of Family is authorized to initiate a family split.";
    public static final String MSG_SPLITTING_MEMBER_NOT_ACTIVE_PREFIX = "Member UUID ";
    public static final String MSG_SPLITTING_MEMBER_NOT_ACTIVE_SUFFIX = " is not an active member of parent family";
    public static final String MSG_NEW_HOF_MUST_BE_SPLITTING_PREFIX = "New family Head of Family (";
    public static final String MSG_NEW_HOF_MUST_BE_SPLITTING_SUFFIX = ") must be among the splitting members";
    public static final String MSG_CANNOT_SPLIT_ALL_MEMBERS = "Cannot split all members from parent family. At least one member must remain in the parent unit.";
    public static final String MSG_SURVIVING_FAMILY_NOT_FOUND_PREFIX = "Surviving family not found: ";
    public static final String MSG_MERGING_FAMILY_NOT_FOUND_PREFIX = "Merging family not found: ";
    public static final String MSG_ONLY_HOF_AUTHORIZED_MERGE = "Only the Head of Family of one of the merging family units is authorized to merge families.";
    public static final String MSG_COMMON_HOF_MUST_BELONG_PREFIX = "Nominated common Head of Family (";
    public static final String MSG_COMMON_HOF_MUST_BELONG_SUFFIX = ") must belong to one of the merging families";
    public static final String MSG_CALLER_ALREADY_MEMBER_PREFIX = "Caller ";
    public static final String MSG_CALLER_ALREADY_MEMBER_SUFFIX = " is already an active member of family ";
    public static final String MSG_CALLER_INFO_REQUIRED_VERIFY = "Authenticated caller information (RequestInfo.userInfo.uuid) is required.";
    public static final String MSG_USER_UUID_REQUIRED_VERIFY = "User UUID is required for identity verification check.";
    public static final String MSG_CITIZEN_NOT_FOUND_REGISTRY_PREFIX = "Citizen profile not found in user registry for UUID: ";
    public static final String MSG_RELATIONSHIP_CACHE_TTL_NOT_CONFIGURED = "family.relationship.cache.ttl is not configured";
    public static final String MSG_RELATIONSHIP_CANNOT_BE_BLANK = "Relationship with Head of Family cannot be blank";
    public static final String MSG_INVALID_RELATIONSHIP_PREFIX = "Invalid relationship: '";
    public static final String MSG_MUST_BE_ONE_OF_SUFFIX = "'. Must be one of: ";
    public static final String MSG_INVALID_CATEGORY_PREFIX = "Invalid socio-economic category: '";
    public static final String MSG_ALLOWED_SUFFIX = "'. Allowed: ";
    public static final String MSG_INVALID_OCCUPATION_PREFIX = "Invalid occupation: '";
    public static final String MSG_INVALID_PROPERTY_TYPE_PREFIX = "Invalid propertyType: '";
    public static final String MSG_INVALID_PROPERTY_USAGE_PREFIX = "Invalid propertyUsage: '";
    public static final String MSG_MDMS_HOST_NOT_CONFIGURED = "egov.mdms.host is not configured";
    public static final String MSG_MDMS_SEARCH_PATH_NOT_CONFIGURED = "egov.mdms.search.path is not configured";
    public static final String MSG_FAMILY_MDMS_MODULE_NOT_CONFIGURED = "family.mdms.module is not configured";
    public static final String MSG_FAMILY_MDMS_RELATIONSHIP_MASTER_NOT_CONFIGURED = "family.mdms.relationship.master is not configured";
    public static final String MSG_TENANT_ID_MANDATORY = "tenantId is mandatory";
}
