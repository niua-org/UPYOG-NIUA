package org.egov.user.domain.service.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Centralized constant repository for user, profile, address, document, and authentication flows.
 * Provides unified definitions for encryption keys, status codes, error identifiers, validation messages,
 * and external service endpoints.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public final class UserConstants {

    private UserConstants() {
        // Utility class - prevent instantiation
    }

    // =========================================================================
    // 1. Encryption Keys & Model Types
    // =========================================================================
    public static final String USER_TYPE_CITIZEN = "CITIZEN";
    public static final String USER_TYPE_EMPLOYEE = "EMPLOYEE";
    public static final String USER_TYPE_SYSTEM = "SYSTEM";
    public static final String USER_ENCRYPTION_KEY = "User";
    public static final String USER_ADDRESS_ENCRYPTION_KEY = "Address";
    public static final String USER_DOCUMENT_ENCRYPTION_KEY = "UserDocument";
    public static final String USER_SELF_ENCRYPTION_KEY = "UserSelf";

    // =========================================================================
    // 2. Status Constants
    // =========================================================================
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String ADDRESS_ACTIVE_STATUS = "active";
    public static final String ADDRESS_INACTIVE_STATUS = "inactive";

    // =========================================================================
    // 3. Error Codes
    // =========================================================================
    public static final String ERROR_INVALID_INIT_REQUEST = "INVALID_INIT_REQUEST";
    public static final String ERROR_TENANT_ID_REQUIRED = "TENANT_ID_REQUIRED";
    public static final String ERROR_MOBILE_NUMBER_REQUIRED = "MOBILE_NUMBER_REQUIRED";
    public static final String ERROR_INVALID_CREATE_REQUEST = "INVALID_CREATE_REQUEST";
    public static final String ERROR_MANDATORY_FIELD_MISSING = "MANDATORY_FIELD_MISSING";
    public static final String ERROR_MANDATORY_DOCUMENT_MISSING = "MANDATORY_DOCUMENT_MISSING";
    public static final String ERROR_INVALID_UPDATE_REQUEST = "INVALID_UPDATE_REQUEST";
    public static final String ERROR_USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String ERROR_INVALID_PROFILE_PAYLOAD = "INVALID_PROFILE_PAYLOAD";
    public static final String ERROR_DATA_MISMATCH = "DATA_MISMATCH";
    public static final String ERROR_NON_EDITABLE_FIELD_MODIFIED = "NON_EDITABLE_FIELD_MODIFIED";
    public static final String ERROR_OTP_MANDATORY_FOR_RESYNC = "OTP_MANDATORY_FOR_RESYNC";
    public static final String ERROR_INVALID_RESYNC_REQUEST = "INVALID_RESYNC_REQUEST";
    public static final String ERROR_INVALID_SEARCH_REQUEST = "INVALID_SEARCH_REQUEST";
    public static final String ERROR_EMPTY_SEARCH_CRITERIA = "EMPTY_SEARCH_CRITERIA";
    public static final String ERROR_USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    public static final String ERROR_INVALID_AUTH_REQUEST = "INVALID_AUTH_REQUEST";
    public static final String ERROR_AUTH_TYPE_REQUIRED = "AUTH_TYPE_REQUIRED";
    public static final String ERROR_OTP_REQUIRED = "OTP_REQUIRED";
    public static final String ERROR_INVALID_OTP = "INVALID_OTP";
    public static final String ERROR_INVALID_MOBILE_NUMBER = "INVALID_MOBILE_NUMBER";
    public static final String ERROR_AADHAAR_NUMBER_REQUIRED = "AADHAAR_NUMBER_REQUIRED";
    public static final String ERROR_INVALID_AADHAAR_NUMBER = "INVALID_AADHAAR_NUMBER";
    public static final String ERROR_AADHAAR_CANNOT_BE_CHANGED = "AADHAAR_CANNOT_BE_CHANGED";
    public static final String ERROR_FAMILY_ID_REQUIRED = "FAMILY_ID_REQUIRED";
    public static final String ERROR_INVALID_FAMILY_ID = "INVALID_FAMILY_ID";
    public static final String ERROR_NO_AUTH_STRATEGY_FOUND = "NO_AUTH_STRATEGY_FOUND";
    public static final String ERROR_MULTIPLE_PROFILES_FOUND = "MULTIPLE_PROFILES_FOUND";
    public static final String ERROR_INVALID_ADDRESS_REQUEST = "INVALID_ADDRESS_REQUEST";
    public static final String ERROR_INVALID_ADDRESS_SEARCH = "INVALID_ADDRESS_SEARCH";
    public static final String ERROR_ADDRESS_NOT_FOUND = "ADDRESS_NOT_FOUND";
    public static final String ERROR_INVALID_DOCUMENT_REQUEST = "INVALID_DOCUMENT_REQUEST";
    public static final String ERROR_INVALID_DOCUMENTS_REQUEST = "INVALID_DOCUMENTS_REQUEST";
    public static final String ERROR_DOCUMENT_NOT_FOUND = "DOCUMENT_NOT_FOUND";
    public static final String ERROR_DOCUMENT_ID_REQUIRED = "DOCUMENT_ID_REQUIRED";
    public static final String ERROR_DOCUMENT_TYPE_REQUIRED = "DOCUMENT_TYPE_REQUIRED";
    public static final String ERROR_FILESTORE_ID_REQUIRED = "FILESTORE_ID_REQUIRED";
    public static final String ERROR_INVALID_SEARCH_CRITERIA = "INVALID_SEARCH_CRITERIA";
    public static final String ERROR_USER_UUID_REQUIRED = "USER_UUID_REQUIRED";
    public static final String ERROR_INVALID_REQUEST = "INVALID_REQUEST";
    public static final String ERROR_INVALID_TENANT_ID = "INVALID_TENANT_ID";
    public static final String ERROR_INVALID_AADHAAR_IDENTIFIER = "INVALID_AADHAAR_IDENTIFIER";
    public static final String ERROR_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    public static final String ERROR_UNSUPPORTED_AUTH_TYPE = "UNSUPPORTED_AUTH_TYPE";
    public static final String ERROR_AADHAAR_VERIFICATION_FAILED = "AADHAAR_VERIFICATION_FAILED";
    public static final String ERROR_AADHAAR_VERIFY_ERROR = "AADHAAR_VERIFY_ERROR";
    public static final String ERROR_USER_IDENTIFIER_REQUIRED = "USER_IDENTIFIER_REQUIRED";
    public static final String ERROR_DUPLICATE_ADDRESS = "DUPLICATE_ADDRESS";
    public static final String ERROR_ADDRESS_ID_REQUIRED = "ADDRESS_ID_REQUIRED";
    public static final String ERROR_INVALID_DELETE_REQUEST = "INVALID_DELETE_REQUEST";
    public static final String ERROR_MAX_OTP_ATTEMPTS_EXCEEDED = "MAX_OTP_ATTEMPTS_EXCEEDED";
    public static final String ERROR_UNAUTHORIZED_RESYNC = "UNAUTHORIZED_RESYNC";
    public static final String ERROR_FILESTORE_NOT_FOUND = "FILESTORE_NOT_FOUND";
    public static final String ERROR_AADHAAR_ALREADY_LINKED = "AADHAAR_ALREADY_LINKED";
    public static final String ERROR_INVALID_ALTERNATE_MOBILE = "INVALID_ALTERNATE_MOBILE";
    public static final String ERROR_ALTERNATE_MOBILE_SAME_AS_PRIMARY = "ALTERNATE_MOBILE_SAME_AS_PRIMARY";
    public static final String ERROR_GUARDIAN_MANDATORY_FOR_MINOR = "GUARDIAN_MANDATORY_FOR_MINOR";
    public static final String ERROR_INVALID_ADDRESS_TYPE = "INVALID_ADDRESS_TYPE";
    public static final String ERROR_FIXED_OTP_NOT_CONFIGURED = "FIXED_OTP_NOT_CONFIGURED";
    public static final String ERROR_ACCOUNT_INACTIVE = "ACCOUNT_INACTIVE";
    public static final String ERROR_UNAUTHORIZED_CITIZEN_SEARCH = "UNAUTHORIZED_CITIZEN_SEARCH";
    public static final String ERROR_UNVERIFIED_CITIZEN_EXISTS = "UNVERIFIED_CITIZEN_EXISTS";

    // =========================================================================
    // 3a. Localization Keys (UG_ standard)
    // =========================================================================
    // Address Mandatory Fields
    public static final String UG_USER_ADDRESS_CITY_MANDATORY = "UG_USER_ADDRESS_CITY_MANDATORY";
    public static final String UG_USER_ADDRESS_LOCALITY_MANDATORY = "UG_USER_ADDRESS_LOCALITY_MANDATORY";
    public static final String UG_USER_ADDRESS_STREET_NAME_MANDATORY = "UG_USER_ADDRESS_STREET_NAME_MANDATORY";
    public static final String UG_USER_ADDRESS_HOUSE_NUMBER_MANDATORY = "UG_USER_ADDRESS_HOUSE_NUMBER_MANDATORY";
    public static final String UG_USER_ADDRESS_LANDMARK_MANDATORY = "UG_USER_ADDRESS_LANDMARK_MANDATORY";
    public static final String UG_USER_ADDRESS_PIN_CODE_MANDATORY = "UG_USER_ADDRESS_PIN_CODE_MANDATORY";
    public static final String UG_USER_ADDRESS_TYPE_MANDATORY = "UG_USER_ADDRESS_TYPE_MANDATORY";

    // Address Non-Editable Fields
    public static final String UG_USER_ADDRESS_NON_EDITABLE_CITY = "UG_USER_ADDRESS_NON_EDITABLE_CITY";
    public static final String UG_USER_ADDRESS_NON_EDITABLE_LOCALITY = "UG_USER_ADDRESS_NON_EDITABLE_LOCALITY";
    public static final String UG_USER_ADDRESS_NON_EDITABLE_HOUSE_NUMBER = "UG_USER_ADDRESS_NON_EDITABLE_HOUSE_NUMBER";
    public static final String UG_USER_ADDRESS_NON_EDITABLE_PIN_CODE = "UG_USER_ADDRESS_NON_EDITABLE_PIN_CODE";

    // Profile Demographics Mandatory Fields
    public static final String UG_USER_PROFILE_NAME_MANDATORY = "UG_USER_PROFILE_NAME_MANDATORY";
    public static final String UG_USER_PROFILE_FATHER_NAME_MANDATORY = "UG_USER_PROFILE_FATHER_NAME_MANDATORY";
    public static final String UG_USER_PROFILE_MOTHER_NAME_MANDATORY = "UG_USER_PROFILE_MOTHER_NAME_MANDATORY";
    public static final String UG_USER_PROFILE_MOBILE_MANDATORY = "UG_USER_PROFILE_MOBILE_MANDATORY";
    public static final String UG_USER_PROFILE_DOB_MANDATORY = "UG_USER_PROFILE_DOB_MANDATORY";
    public static final String UG_USER_PROFILE_EMAIL_MANDATORY = "UG_USER_PROFILE_EMAIL_MANDATORY";
    public static final String UG_USER_PROFILE_GENDER_MANDATORY = "UG_USER_PROFILE_GENDER_MANDATORY";
    public static final String UG_USER_PROFILE_ADDRESS_MANDATORY = "UG_USER_PROFILE_ADDRESS_MANDATORY";

    // Profile Non-Editable Fields
    public static final String UG_USER_PROFILE_NON_EDITABLE_MOBILE = "UG_USER_PROFILE_NON_EDITABLE_MOBILE";
    public static final String UG_USER_PROFILE_NON_EDITABLE_FATHER_NAME = "UG_USER_PROFILE_NON_EDITABLE_FATHER_NAME";
    public static final String UG_USER_PROFILE_NON_EDITABLE_MOTHER_NAME = "UG_USER_PROFILE_NON_EDITABLE_MOTHER_NAME";
    public static final String UG_USER_PROFILE_NON_EDITABLE_EMAIL = "UG_USER_PROFILE_NON_EDITABLE_EMAIL";
    public static final String UG_USER_PROFILE_NON_EDITABLE_NAME = "UG_USER_PROFILE_NON_EDITABLE_NAME";
    public static final String UG_USER_PROFILE_NON_EDITABLE_DOB = "UG_USER_PROFILE_NON_EDITABLE_DOB";
    public static final String UG_USER_PROFILE_NON_EDITABLE_GENDER = "UG_USER_PROFILE_NON_EDITABLE_GENDER";

    // Profile Document Mandatory Fields
    public static final String UG_USER_DOC_ADDRESS_PROOF_MANDATORY = "UG_USER_DOC_ADDRESS_PROOF_MANDATORY";
    public static final String UG_USER_DOC_IDENTITY_PROOF_MANDATORY = "UG_USER_DOC_IDENTITY_PROOF_MANDATORY";
    public static final String UG_USER_DOC_DOB_PROOF_MANDATORY = "UG_USER_DOC_DOB_PROOF_MANDATORY";

    // Regular expressions
    public static final String REGEX_INDIAN_MOBILE = "^[6-9][0-9]{9}$";

    // Master data defaults
    public static final String DEFAULT_ALLOWED_ADDRESS_TYPES = "PERMANENT,CORRESPONDENCE,OTHER";
    public static final String MDMS_ADDRESS_TYPE_MASTER_NAME = "AddressType";
    public static final String MDMS_ADDRESS_TYPE_MODULE_NAME = "common-masters";

    // =========================================================================
    // 4. Validation Error Messages
    // =========================================================================
    public static final String MSG_INIT_BODY_NULL = "Profile init request body cannot be null";
    public static final String MSG_INIT_TENANT_REQUIRED = "tenantId is mandatory for profile initialization";
    public static final String MSG_INIT_MOBILE_REQUIRED = "mobileNumber is mandatory for profile initialization";
    public static final String MSG_CREATE_BODY_NULL = "Profile create request body cannot be null";
    public static final String MSG_CREATE_TENANT_REQUIRED = "tenantId is mandatory for profile creation";

    // Section 8 Mandatory Demographic Messages
    public static final String MSG_NAME_MANDATORY = "Name is mandatory for profile creation.";
    public static final String MSG_FATHER_NAME_MANDATORY = "Father's Name is mandatory for profile creation.";
    public static final String MSG_MOTHER_NAME_MANDATORY = "Mother's Name is mandatory for profile creation.";
    public static final String MSG_MOBILE_MANDATORY = "Mobile Number is mandatory for profile creation.";
    public static final String MSG_DOB_MANDATORY = "Date of Birth is mandatory for profile creation.";
    public static final String MSG_EMAIL_MANDATORY = "Email ID is mandatory for profile creation.";
    public static final String MSG_GENDER_MANDATORY = "Gender is mandatory for profile creation.";
    public static final String MSG_ADDRESS_MANDATORY = "Address details are mandatory for profile creation.";

    // Section 8 Mandatory Document Messages
    public static final String MSG_ADDRESS_PROOF_MANDATORY = "Upload Address Proof is mandatory for profile creation.";
    public static final String MSG_IDENTITY_PROOF_MANDATORY = "Upload Identity Proof is mandatory for profile creation.";
    public static final String MSG_DOB_PROOF_MANDATORY = "Upload D.O.B. Proof is mandatory for profile creation.";

    // Section 8 Mandatory Address Field Messages
    public static final String MSG_CITY_MANDATORY = "City is mandatory for address creation.";
    public static final String MSG_LOCALITY_MANDATORY = "Locality is mandatory for address creation.";
    public static final String MSG_STREET_NAME_MANDATORY = "Street Name is mandatory for address creation.";
    public static final String MSG_HOUSE_NUMBER_MANDATORY = "House Number is mandatory for address creation.";
    public static final String MSG_LANDMARK_MANDATORY = "Landmark is mandatory for address creation.";
    public static final String MSG_PIN_CODE_MANDATORY = "Pin Code is mandatory for address creation.";
    public static final String MSG_ADDRESS_TYPE_MANDATORY = "Address Type is mandatory for address creation.";
    public static final String MSG_INVALID_ADDRESS_TYPE = "Address type is not valid according to configured master data.";

    // Section 8 Non-Editable Field Modification Messages
    public static final String MSG_NON_EDITABLE_MOBILE = "Mobile Number is non-editable and cannot be changed.";
    public static final String MSG_NON_EDITABLE_FATHER_NAME = "Father's Name is non-editable and cannot be changed.";
    public static final String MSG_NON_EDITABLE_MOTHER_NAME = "Mother's Name is non-editable and cannot be changed.";
    public static final String MSG_NON_EDITABLE_EMAIL = "Email ID is non-editable and cannot be changed.";
    public static final String MSG_NON_EDITABLE_NAME = "Name is non-editable and cannot be changed. Please use 'Re-sync with Aadhaar' if your official name has changed.";
    public static final String MSG_NON_EDITABLE_DOB = "Date of Birth is non-editable and cannot be changed. Please use 'Re-sync with Aadhaar' if your official DOB has changed.";
    public static final String MSG_NON_EDITABLE_GENDER = "Gender is non-editable and cannot be changed. Please use 'Re-sync with Aadhaar' if your official Gender has changed.";

    public static final String MSG_NON_EDITABLE_CITY = "City is non-editable and cannot be changed for an address.";
    public static final String MSG_NON_EDITABLE_LOCALITY = "Locality is non-editable and cannot be changed for an address.";
    public static final String MSG_NON_EDITABLE_HOUSE_NUMBER = "House Number is non-editable and cannot be changed for an address.";
    public static final String MSG_NON_EDITABLE_PIN_CODE = "Pin Code is non-editable and cannot be changed for an address.";

    public static final String MSG_OTP_MANDATORY_FOR_RESYNC = "OTP is mandatory for Aadhaar re-sync operation";
    public static final String MSG_EMPTY_SEARCH_CRITERIA = "At least one search filter (tenantId, uuid, id, mobileNumber, or aadhaarNumber) must be provided";
    public static final String MSG_AADHAAR_MANDATORY_FOR_RESYNC = "12-digit Aadhaar number is mandatory for Aadhaar re-sync";
    public static final String MSG_AADHAAR_MISMATCH = "Cannot re-sync profile with a different Aadhaar number than previously registered";
    public static final String MSG_USER_IDENTIFIER_REQUIRED = "uuid, id, or mobileNumber is required to locate citizen";
    public static final String MSG_USER_ALREADY_EXISTS_AADHAAR = "A user profile already exists with this Aadhaar number";
    public static final String MSG_USER_ALREADY_EXISTS_DEMO = "A user profile with the same name and date of birth already exists for this mobile number";
    public static final String MSG_UPDATE_BODY_NULL = "Profile update request or profile payload cannot be null";
    public static final String MSG_RESYNC_BODY_NULL = "Re-sync request cannot be null";
    public static final String MSG_LOGIN_REQUEST_NULL = "LoginV2Request payload cannot be null";
    public static final String MSG_AUTH_TYPE_NULL = "authType cannot be null";
    public static final String MSG_LOGIN_TENANT_REQUIRED = "tenantId is mandatory for loginv2 authentication";
    public static final String MSG_LOGIN_OTP_REQUIRED = "otp is mandatory for authentication";
    public static final String MSG_MOBILE_AUTH_MOBILE_REQUIRED = "mobileNumber is required for MOBILE channel authentication";
    public static final String MSG_AADHAAR_IDENTIFIER_REQUIRED = "At least one of aadhaarNumber, txnId, or referenceId is required for AADHAAR channel authentication";
    public static final String MSG_FAMILY_ID_REQUIRED = "familyId is required for FAMILY_ID channel authentication";
    public static final String MSG_FAMILY_MEMBER_MOBILE_REQUIRED = "Member mobileNumber is required for FAMILY_ID channel authentication";
    public static final String MSG_SELECTED_USER_NOT_FOUND = "Selected citizen account not found";
    public static final String MSG_AUTH_FAILED_NULL_USER = "Failed to resolve authenticated user";
    public static final String MSG_UNSUPPORTED_AUTH_CREDENTIALS = "No authentication strategy found capable of handling the provided credentials";
    public static final String MSG_UNSUPPORTED_AUTH_CHANNEL = "No authentication strategy found for channel: ";
    public static final String MSG_DOCUMENTS_PAYLOAD_EMPTY = "Documents payload cannot be empty";
    public static final String MSG_DOCUMENT_BODY_NULL = "Document body cannot be null for update";
    public static final String MSG_DOCUMENT_ID_REQUIRED = "Document ID is required for update";
    public static final String MSG_DOCUMENT_DELETE_REQUEST_NULL = "Delete document request cannot be null";
    public static final String MSG_DOCUMENT_DELETE_ID_REQUIRED = "documentId is mandatory for delete";
    public static final String MSG_DOCUMENT_TYPE_MANDATORY = "documentType is mandatory";
    public static final String MSG_FILESTORE_ID_MANDATORY = "filestoreId is mandatory";
    public static final String MSG_DOCUMENT_SEARCH_CRITERIA_NULL = "Search criteria cannot be null";
    public static final String MSG_DOCUMENT_USER_UUID_MANDATORY = "uuid is mandatory for document association";
    public static final String MSG_AADHAAR_VERIFICATION_REJECTED = "Aadhaar verification returned not verified";
    public static final String MSG_AADHAAR_VERIFY_PROVIDER_ERROR = "Failed to verify Aadhaar OTP with identity provider";
    public static final String MSG_ADDRESS_REQUEST_NULL = "Address request and address body cannot be null";
    public static final String MSG_DUPLICATE_ADDRESS_EXISTS = "An active address with the same details already exists for this user.";
    public static final String MSG_DUPLICATE_ADDRESS_IDENTICAL = "The provided address details are identical to the existing active address.";
    public static final String MSG_ADDRESS_ID_REQUIRED_UPDATE = "addressId is mandatory for updating address";
    public static final String MSG_ADDRESS_ID_REQUIRED_DELETE = "addressId is mandatory to delete address";
    public static final String MSG_ADDRESS_SEARCH_CRITERIA_NULL = "Address search criteria cannot be null";
    public static final String MSG_ADDRESS_DELETE_REQUEST_NULL = "Delete request cannot be null";
    public static final String MSG_ADDRESS_USER_UUID_MANDATORY = "uuid is mandatory for address operation";
    public static final String MSG_MAX_OTP_ATTEMPTS_EXCEEDED = "Maximum invalid OTP attempts exceeded. Transaction reference ID is locked. Please request a new OTP.";
    public static final String MSG_UNAUTHORIZED_RESYNC = "Unauthorized re-sync request: Authenticated user does not match target citizen profile.";
    public static final String MSG_FILESTORE_NOT_FOUND = "The provided filestoreId does not exist or is inaccessible in filestore service: ";
    public static final String MSG_AADHAAR_ALREADY_LINKED = "This Aadhaar number is already linked to another citizen profile. An Aadhaar card cannot be shared across multiple citizen identities.";
    public static final String MSG_UNVERIFIED_CITIZEN_EXISTS = "Only one citizen profile with unverified identity is allowed per mobile number. All other citizens under this mobile number must be identity verified.";
    public static final String MSG_INVALID_ALTERNATE_MOBILE = "Alternate mobile number must be a valid 10-digit Indian phone number starting with 6-9.";
    public static final String MSG_ALTERNATE_MOBILE_SAME_AS_PRIMARY = "Alternate mobile number cannot be the same as primary mobile number.";
    public static final String MSG_GUARDIAN_MANDATORY_FOR_MINOR = "Guardian Name and Guardian Relation are mandatory for minor citizens under 18 years of age.";
    public static final String MSG_FIXED_OTP_NOT_CONFIGURED = "Citizen login fixed OTP value is not configured";
    public static final String MSG_ACCOUNT_INACTIVE = "Citizen account is inactive. Please contact support.";
    public static final String MSG_UNAUTHORIZED_CITIZEN_SEARCH = "Access denied: Citizens are only permitted to search for their own profile. UUID or mobile number must match the authenticated user.";

    // =========================================================================
    // 5. API Identifiers & Response Message Constants
    // =========================================================================
    public static final String API_ID_LOGIN_V2 = "egov-user-loginv2";
    public static final String VERSION_2_0 = "2.0";
    public static final String RES_MSG_ID_LOGIN_SUCCESS = "uip-loginv2-success";
    public static final String RES_MSG_ID_MULTIPLE_USERS = "uip-loginv2-multiple-users";

    // =========================================================================
    // 6. Document Type Categories
    // =========================================================================
    public static final String DOC_TYPE_ADDRESS_PROOF = "ADDRESS_PROOF";
    public static final String DOC_TYPE_IDENTITY_PROOF = "IDENTITY_PROOF";
    public static final String DOC_TYPE_DOB_PROOF = "DOB_PROOF";

    public static final List<String> ADDRESS_PROOF_TYPES = Collections.unmodifiableList(Arrays.asList(
            "ADDRESS_PROOF", "ADDRESS", "ELECTRICITY_BILL", "WATER_BILL", "RENT_AGREEMENT", "PROPERTY_TAX_RECEIPT"
    ));

    public static final List<String> IDENTITY_PROOF_TYPES = Collections.unmodifiableList(Arrays.asList(
            "IDENTITY_PROOF", "IDENTITY", "AADHAAR", "PAN", "VOTER_ID", "DRIVING_LICENSE", "PASSPORT"
    ));

    public static final List<String> DOB_PROOF_TYPES = Collections.unmodifiableList(Arrays.asList(
            "DOB_PROOF", "DOB", "BIRTH_CERTIFICATE", "PAN", "PASSPORT", "SCHOOL_LEAVING_CERTIFICATE"
    ));

    // =========================================================================
    // 7. External Service Endpoints & Integration Keys
    // =========================================================================
    public static final String AADHAAR_SEND_OTP_PATH = "/aadhaar/v1/_sendOtp";
    public static final String AADHAAR_VERIFY_OTP_PATH = "/aadhaar/v1/_verifyOtp";

    public static final String JSON_KEY_DEMOGRAPHICS = "demographics";
    public static final String JSON_KEY_NAME = "name";
    public static final String JSON_KEY_GENDER = "gender";
    public static final String JSON_KEY_DOB = "dob";
    public static final String JSON_KEY_PHOTO = "photo";
    public static final String JSON_KEY_MASKED_AADHAAR = "maskedAadhaar";

    // =========================================================================
    // 8. Identity Verification Sources & Multi-Auth Constants
    // =========================================================================
    public static final String IDENTITY_SOURCE_AADHAAR = "AADHAAR";
    public static final String IDENTITY_SOURCE_DIGILOCKER = "DIGILOCKER";
    public static final String IDENTITY_SOURCE_FAMILY_ID = "FAMILY_ID";
    public static final String IDENTITY_SOURCE_SYSTEM = "SYSTEM";
    public static final String AADHAAR_USERNAME_PREFIX = "AADHAAR_";
    public static final String DEFAULT_AADHAAR_CITIZEN_NAME = "Aadhaar Verified Citizen";
    public static final String DEFAULT_REQUESTER_AADHAAR_VERIFY_ENDPOINT = "requester-services-dx/aadhaar/v1/_verifyOtp";
}
