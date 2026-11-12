package org.egov.user.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.*;
import org.egov.user.domain.exception.InvalidUserCreateException;
import org.egov.user.domain.exception.InvalidUserUpdateException;
import org.egov.user.domain.model.enums.BloodGroup;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.model.enums.GuardianRelation;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.utils.UserConstants;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Core domain entity representing a citizen or employee user in UPYOG.
 * Encapsulates demographics, roles, addresses, audit timestamps, and authentication state.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
public class User {

    private Long id;
    private String uuid;
    private String digilockerid;
    private String access_token;
    private String scope;

    @Pattern(regexp = UserServiceConstants.PATTERN_TENANT)
    @Size(max = 50)
    private String tenantId;
    private String username;
    private String title;
    private String password;
    private String salutation;

    @Pattern(regexp = UserServiceConstants.PATTERN_NAME)
    private String guardian;

    private GuardianRelation guardianRelation;

    @Pattern(regexp = UserServiceConstants.PATTERN_NAME)
    @Size(max = 50)
    private String name;
    private Gender gender;
    private String mobileNumber;

    @Email
    private String emailId;
    private String altContactNumber;
    private String pan;
    private String aadhaarNumber;
    private Address permanentAddress;
    private Address correspondenceAddress;
    private Set<Address> addresses;
    private List<UserDocument> documents;
    private Boolean active;
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    private Date dob;
    private Date passwordExpiryDate;
    @Builder.Default
    private String locale = "en_IN";
    private UserType type;
    private BloodGroup bloodGroup;
    private String identificationMark;
    private String signature;
    private String photo;
    private Boolean accountLocked;
    private Long accountLockedDate;
    private Date lastModifiedDate;
    private Date createdDate;
    private String otpReference;
    private Long createdBy;
    private Long lastModifiedBy;
    private Long loggedInUserId;
    private String loggedInUserUuid;
    private boolean otpValidationMandatory;
    @Builder.Default
    private boolean mobileValidationMandatory = true;
    private String alternateMobileNumber;
    private boolean digilockerRegistration;

    @Pattern(regexp = UserServiceConstants.PATTERN_NAME)
    @Size(max = 100)
    private String fatherName;

    @Pattern(regexp = UserServiceConstants.PATTERN_NAME)
    @Size(max = 100)
    private String motherName;

    @Pattern(regexp = UserServiceConstants.PATTERN_NAME)
    @Size(max = 100)
    private String spouseName;

    @Size(max = 32)
    private String caste;

    private Boolean isSpeciallyAbled;

    private Boolean isIdentityVerified;
    private Date identityVerifiedDate;
    private String identityVerifiedSource;
    private Date lastResyncDate;
    private Integer edittedCount;
    private String lastLoginMethod;
    private String familyId;

    /**
     * Appends an {@link Address} to this user's address collection.
     *
     * @param addressItem address to add
     * @return current user instance
     */
    public User addAddressItem(Address addressItem) {
        if (this.addresses == null) {
            this.addresses = new HashSet<>();
        }
        this.addresses.add(addressItem);
        return this;
    }

    /**
     * Appends a {@link Role} to this user's role collection.
     *
     * @param roleItem role to add
     * @return current user instance
     */
    public User addRolesItem(Role roleItem) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(roleItem);
        return this;
    }

    /**
     * Validates that all baseline mandatory attributes are present for user creation, enforcing name validation by default.
     *
     * @throws InvalidUserCreateException if mandatory fields (username, name, mobile, active, type, roles, tenantId, or OTP) are missing
     */
    public void validateNewUser() {
        validateNewUser(true);
    }

    /**
     * Validates that all baseline mandatory attributes are present for new user registration.
     *
     * @param createUserValidateName whether user {@code name} must be validated for presence
     * @throws InvalidUserCreateException if mandatory fields are missing
     */
    public void validateNewUser(boolean createUserValidateName) {
        if (isUsernameAbsent()
                || (createUserValidateName && isNameAbsent())
                || isMobileNumberAbsent()
                || isActiveIndicatorAbsent()
                || isTypeAbsent()
                || isRolesAbsent()
                || isOtpReferenceAbsent()
                || isTenantIdAbsent()) {
            throw new InvalidUserCreateException(this);
        }
    }

    /**
     * Validates user registration attributes for the V2 API, taking into account nested address array structures.
     *
     * @param createUserValidateName whether user {@code name} must be validated for presence
     * @throws InvalidUserCreateException if required fields are missing
     */
    public void validateNewUserV2(boolean createUserValidateName) {
        if (isUsernameAbsent()
                || (createUserValidateName && isNameAbsent())
                || isMobileNumberAbsent()
                || isActiveIndicatorAbsent()
                || isTypeAbsent()
                || isRolesAbsent()
                || isOtpReferenceAbsent()
                || isTenantIdAbsent()) {
            throw new InvalidUserCreateException(this);
        }
    }

    /**
     * Simple validation for the V2 API that validates user fields while skipping legacy address format checks.
     *
     * @param createUserValidateName whether user {@code name} must be validated for presence
     * @throws InvalidUserCreateException if mandatory fields are missing
     */
    public void validateNewUserV2Simple(boolean createUserValidateName) {
        if (isUsernameAbsent()
                || (createUserValidateName && isNameAbsent())
                || isMobileNumberAbsent()
                || isActiveIndicatorAbsent()
                || isTypeAbsent()
                || isRolesAbsent()
                || isOtpReferenceAbsent()
                || isTenantIdAbsent()) {
            throw new InvalidUserCreateException(this);
        }
    }

    /**
     * Validates that the mandatory {@code tenantId} attribute is present prior to committing user modifications.
     *
     * @throws InvalidUserUpdateException if {@code tenantId} is absent or empty
     */
    public void validateUserModification() {
        if (isTenantIdAbsent()) {
            throw new InvalidUserUpdateException(this);
        }
    }

    /**
     * Validates that all mandatory fields specified in Section 8 are present for citizen profile creation.
     * Mandatory fields: Name, Father's Name, Mother's Name, Mobile Number, DOB, Email ID, Gender.
     *
     * @throws CustomException if any mandatory field is missing
     */
    public void validateMandatoryProfileFields() {
        if (StringUtils.isBlank(this.name)) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_NAME_MANDATORY, UserConstants.MSG_NAME_MANDATORY);
        }
        if (StringUtils.isBlank(this.fatherName)) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_FATHER_NAME_MANDATORY, UserConstants.MSG_FATHER_NAME_MANDATORY);
        }
        if (StringUtils.isBlank(this.motherName)) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_MOTHER_NAME_MANDATORY, UserConstants.MSG_MOTHER_NAME_MANDATORY);
        }
        if (StringUtils.isBlank(this.mobileNumber)) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_MOBILE_MANDATORY, UserConstants.MSG_MOBILE_MANDATORY);
        }
        if (this.dob == null) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_DOB_MANDATORY, UserConstants.MSG_DOB_MANDATORY);
        }
        if (StringUtils.isBlank(this.emailId)) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_EMAIL_MANDATORY, UserConstants.MSG_EMAIL_MANDATORY);
        }
        if (this.gender == null) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_GENDER_MANDATORY, UserConstants.MSG_GENDER_MANDATORY);
        }
    }

    /**
     * Enforces Section 8 non-editable field constraints during profile updates.
     * Non-editable fields: Name, Father's Name, Mother's Name, Mobile Number, DOB, Email ID, Gender.
     * These cannot be changed once set, unless authorized via Aadhaar re-sync (for Name, DOB, Gender).
     * Editable fields (Spouse Name, Guardian Name, Alternate Mobile Number, Profile Picture) are permitted.
     *
     * @param existingUser the existing user entity from database
     * @param isResync     flag indicating whether update is authenticated via Aadhaar re-sync
     * @throws CustomException if any non-editable field is illegally modified
     */
    public void validateNonEditableProfileFields(User existingUser, boolean isResync) {
        if (existingUser == null) {
            return;
        }

        // 1. Mobile Number (Non-Editable)
        if (isFieldModified(this.mobileNumber, existingUser.getMobileNumber())) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_MOBILE, UserConstants.MSG_NON_EDITABLE_MOBILE);
        }

        // 2. Father's Name (Non-Editable)
        if (isFieldModified(this.fatherName, existingUser.getFatherName())) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_FATHER_NAME, UserConstants.MSG_NON_EDITABLE_FATHER_NAME);
        }

        // 3. Mother's Name (Non-Editable)
        if (isFieldModified(this.motherName, existingUser.getMotherName())) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_MOTHER_NAME, UserConstants.MSG_NON_EDITABLE_MOTHER_NAME);
        }

        // 4. Email ID (Non-Editable)
        if (isFieldModified(this.emailId, existingUser.getEmailId())) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_EMAIL, UserConstants.MSG_NON_EDITABLE_EMAIL);
        }

        // 5. Name (Non-Editable unless Aadhaar re-sync)
        if (!isResync && isFieldModified(this.name, existingUser.getName())) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_NAME, UserConstants.MSG_NON_EDITABLE_NAME);
        }

        // 6. Date of Birth (Non-Editable unless Aadhaar re-sync)
        if (!isResync && existingUser.getDob() != null && this.dob != null
                && !DateUtils.isSameDay(this.dob, existingUser.getDob())) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_DOB, UserConstants.MSG_NON_EDITABLE_DOB);
        }

        // 7. Gender (Non-Editable unless Aadhaar re-sync)
        if (!isResync && existingUser.getGender() != null && this.gender != null
                && !existingUser.getGender().equals(this.gender)) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_GENDER, UserConstants.MSG_NON_EDITABLE_GENDER);
        }
    }

    /**
     * Determines whether an incoming string field has been modified relative to the existing record.
     *
     * @param incoming the incoming field value
     * @param existing the existing field value from database
     * @return {@code true} if both values are non-blank and differ ignoring case; {@code false} otherwise
     */
    private boolean isFieldModified(String incoming, String existing) {
        if (StringUtils.isBlank(existing)) {
            return false;
        }
        if (StringUtils.isBlank(incoming)) {
            return false;
        }
        return !incoming.trim().equalsIgnoreCase(existing.trim());
    }

    /**
     * Validates whether the correspondence address is invalid.
     *
     * @return {@code true} if correspondence address is provided and fails validity check; {@code false} otherwise
     */
    @JsonIgnore
    public boolean isCorrespondenceAddressInvalid() {
        return correspondenceAddress != null && correspondenceAddress.isInvalid();
    }

    /**
     * Validates whether the permanent address is invalid.
     *
     * @return {@code true} if permanent address is provided and fails validity check; {@code false} otherwise
     */
    @JsonIgnore
    public boolean isPermanentAddressInvalid() {
        return permanentAddress != null && permanentAddress.isInvalid();
    }

    /**
     * Validates whether any attached address in the collection is invalid.
     *
     * @return {@code true} if any address in the list is non-null and invalid; {@code false} otherwise
     */
    @JsonIgnore
    public boolean isAddressesInvalid() {
        if (addresses == null || addresses.isEmpty()) {
            return false; // Addresses are optional in v2
        }
        return addresses.stream().anyMatch(address -> address != null && address.isInvalid());
    }

    /**
     * Checks if OTP reference is absent when OTP validation is configured as mandatory.
     *
     * @return {@code true} if OTP validation is required but reference token is empty
     */
    @JsonIgnore
    public boolean isOtpReferenceAbsent() {
        return otpValidationMandatory && isEmpty(otpReference);
    }

    /**
     * Checks if the user classification type (e.g., CITIZEN, EMPLOYEE) is absent.
     *
     * @return {@code true} if user type is null or empty
     */
    @JsonIgnore
    public boolean isTypeAbsent() {
        return isEmpty(type);
    }

    /**
     * Checks if the active account status flag is absent.
     *
     * @return {@code true} if active flag is null
     */
    @JsonIgnore
    public boolean isActiveIndicatorAbsent() {
        return isEmpty(active);
    }

    /**
     * Checks if the mobile number is absent when mobile validation is mandatory.
     *
     * @return {@code true} if mobile validation is mandatory and mobile number is empty
     */
    @JsonIgnore
    public boolean isMobileNumberAbsent() {
        return mobileValidationMandatory && isEmpty(mobileNumber);
    }

    /**
     * Checks if the user primary display name is absent.
     *
     * @return {@code true} if name is null or empty
     */
    @JsonIgnore
    public boolean isNameAbsent() {
        return isEmpty(name);
    }

    /**
     * Checks if the unique login username is absent.
     *
     * @return {@code true} if username is null or empty
     */
    @JsonIgnore
    public boolean isUsernameAbsent() {
        return isEmpty(username);
    }

    /**
     * Checks if the tenant identifier is absent.
     *
     * @return {@code true} if tenantId is null or empty
     */
    @JsonIgnore
    public boolean isTenantIdAbsent() {
        return isEmpty(tenantId);
    }

    /**
     * Checks if the credential password is absent.
     *
     * @return {@code true} if password is null or empty
     */
    @JsonIgnore
    public boolean isPasswordAbsent() {
        return isEmpty(password);
    }

    /**
     * Checks if user authorization roles are absent or contain empty codes.
     *
     * @return {@code true} if roles collection is empty or any role code is missing
     */
    @JsonIgnore
    public boolean isRolesAbsent() {
        return CollectionUtils.isEmpty(roles) || roles.stream().anyMatch(r -> isEmpty(r.getCode()));
    }

    /**
     * Checks if the numeric primary key id is absent.
     *
     * @return {@code true} if id is null
     */
    @JsonIgnore
    public boolean isIdAbsent() {
        return id == null;
    }

    /**
     * Clears sensitive or immutable security fields prior to executing a partial profile update.
     * <p>
     * Nullifies {@code username}, {@code type}, {@code mobileNumber}, {@code password},
     * {@code passwordExpiryDate}, and account lock fields. Roles are preserved to maintain
     * gateway RBAC authorization compatibility.
     */
    public void nullifySensitiveFields() {
        username = null;
        type = null;
        mobileNumber = null;
        password = null;
        passwordExpiryDate = null;
        // CRITICAL: Do NOT nullify roles - they are required for gateway RBAC authorization
        // Setting roles to null causes validation errors in gateway with @Size(min=1) constraint
        // roles = null;
        accountLocked = null;
        accountLockedDate = null;
    }

    /**
     * Checks if the currently logged-in caller's identity mismatches this user entity.
     *
     * @return {@code true} if the logged-in user's ID or UUID differs from this user's ID or UUID
     */
    @JsonIgnore
    public boolean isLoggedInUserDifferentFromUpdatedUser() {
        return !id.equals(loggedInUserId) || !uuid.equals(loggedInUserUuid);
    }

    /**
     * Enriches this user entity with standard Citizen classification:
     * sets {@code type = UserType.CITIZEN} and assigns the default {@code CITIZEN} role.
     */
    public void setRoleToCitizen() {
        type = UserType.CITIZEN;
        roles = Collections.singleton(Role.getCitizenRole());
    }

    /**
     * Replaces the current password hash with a newly encoded password.
     *
     * @param newPassword encoded password hash
     */
    public void updatePassword(String newPassword) {
        password = newPassword;
    }

    /**
     * Constructs an {@link OtpValidationRequest} domain object using this user's credentials.
     *
     * @return populated {@link OtpValidationRequest}
     */
    @JsonIgnore
    public OtpValidationRequest getOtpValidationRequest() {
        return OtpValidationRequest.builder()
                .mobileNumber(mobileNumber)
                .tenantId(tenantId)
                .otpReference(otpReference)
                .build();
    }

    /**
     * Extracts non-empty permanent and correspondence addresses as a combined list.
     *
     * @return list containing non-empty correspondence and permanent {@link Address} objects
     */
    @JsonIgnore
    public List<Address> getPermanentAndCorrespondenceAddresses() {
        final ArrayList<Address> addresses = new ArrayList<>();
        if (correspondenceAddress != null && correspondenceAddress.isNotEmpty()) {
            addresses.add(correspondenceAddress);
        }
        if (permanentAddress != null && permanentAddress.isNotEmpty()) {
            addresses.add(permanentAddress);
        }
        return addresses;
    }

    /**
     * Calculates and initializes the password expiration date if not already explicitly set.
     *
     * @param expiryInDays number of days until password expires
     */
    public void setDefaultPasswordExpiry(int expiryInDays) {
        if (passwordExpiryDate == null) {
            passwordExpiryDate = DateUtils.addDays(new Date(), expiryInDays);
        }
    }

    /**
     * Sets the active status flag of this user.
     *
     * @param isActive whether the user account is active
     */
    public void setActive(boolean isActive) {
        active = isActive;
    }

    /**
     * Copies all user attributes, demographic details, and verification metadata from the source into a new {@link User} instance.
     *
     * @param u source domain user entity
     * @return new {@link User} populated with copied fields
     */
    public User toUser(User u) {
   	 
        User user = new User();
 
        user.setId(u.id);
        user.setUuid(u.uuid);
        user.setTenantId(u.tenantId);
        user.setUsername(u.username);
        user.setSalutation(u.salutation);
        user.setName(u.name);
        user.setMobileNumber(u.mobileNumber);
        user.setEmailId(u.emailId);
        user.setAltContactNumber(u.altContactNumber);
        user.setPan(u.pan);
        user.setAadhaarNumber(u.aadhaarNumber);
 
        // Gender conversion
        if (u.gender != null) {
            try {
                user.setGender(u.gender);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid gender value: " + u.gender);
            }
        }
 
        // BloodGroup conversion
        if (u.bloodGroup != null) {
            try {
                user.setBloodGroup(u.bloodGroup);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid blood group value: " + u.bloodGroup);
            }
        }
 
        user.setLocale(u.locale);
        user.setType(u.type);
        user.setAccountLocked(u.accountLocked);
        user.setAccountLockedDate(u.accountLockedDate);
        user.setDob(u.dob);
        user.setPasswordExpiryDate(u.getPasswordExpiryDate());
        user.setSignature(u.signature);
        user.setPhoto(u.photo);
        user.setIdentificationMark(u.identificationMark);
        user.setCreatedBy(u.createdBy);
        user.setCreatedDate(u.createdDate);
        user.setLastModifiedBy(u.lastModifiedBy);
        user.setLastModifiedDate(u.lastModifiedDate);
//        user.setGuardian(u.fatherOrHusbandName);
//        user.setGuardianRelation(u.relationship);
        user.setActive(u.active);
        user.setAlternateMobileNumber(u.alternateMobileNumber);
        user.setFatherName(u.fatherName);
        user.setMotherName(u.motherName);
        user.setSpouseName(u.spouseName);
        user.setIsIdentityVerified(u.isIdentityVerified);
        user.setIdentityVerifiedDate(u.identityVerifiedDate);
        user.setIdentityVerifiedSource(u.identityVerifiedSource);
        user.setLastResyncDate(u.lastResyncDate);
        user.setEdittedCount(u.edittedCount);
        user.setLastLoginMethod(u.lastLoginMethod);
 
        // Address mappings
//        if (u.permanentAddress != null || u.permanentCity != null || u.permanentPinCode != null) {
//            Address permanentAddress = new Address();
//            permanentAddress.setAddress(u.permanentAddress);
//            permanentAddress.setCity(u.permanentCity);
//            permanentAddress.setPinCode(u.permanentPinCode);
//            user.setPermanentAddress(permanentAddress);
//        }
 
//        if (u.correspondenceAddress != null || u.correspondenceCity != null || u.correspondencePinCode != null) {
//            Address correspondenceAddress = new Address();
//            correspondenceAddress.setAddress(u.correspondenceAddress);
//            correspondenceAddress.setCity(u.correspondenceCity);
//            correspondenceAddress.setPinCode(u.correspondencePinCode);
//            user.setCorrespondenceAddress(correspondenceAddress);
//        }
 
        user.setAddresses(u.addresses);
 
        // Roles conversion
        if (u.roles != null) {
            Set<Role> roleEntities = u.roles.stream()
                    .map(roleRequest -> {
                        Role role = new Role();
                        role.setCode(roleRequest.getCode());
                        role.setName(roleRequest.getName());
                        return role;
                    })
                    .collect(Collectors.toSet());
            user.setRoles(roleEntities);
        } else {
            // CRITICAL FIX: Ensure roles is never null, always initialize to empty set
            user.setRoles(new HashSet<>());
        }

        return user;
    }

    /**
     * CRITICAL: Override getRoles to ensure it never returns null
     * This prevents NPE and validation errors in gateway RBAC filter
     * @return Set of roles, never null (returns empty set if roles is null)
     */
    public Set<Role> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        return roles;
    }

    /**
     * CRITICAL: Override setRoles to prevent setting null
     * If null is passed, set empty HashSet instead
     * @param roles Set of roles to set
     */
    public void setRoles(Set<Role> roles) {
        if (roles == null) {
            this.roles = new HashSet<>();
        } else {
            this.roles = roles;
        }
    }
}


