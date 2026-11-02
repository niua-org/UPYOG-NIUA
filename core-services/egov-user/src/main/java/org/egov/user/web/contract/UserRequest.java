package org.egov.user.web.contract;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.egov.user.config.*;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.enums.*;
import org.egov.user.web.contract.deserializer.MultiFormatDateDeserializer;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contract payload for creating or updating user records via HTTP requests.
 * Contains demographic details, guardian information, identity verification flags, and credentials.
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    private Long id;

    @Size(max = 64)
    private String userName;

    @Size(max = 40)
    private String access_token;

    private String scope;

    @Size(max = 5)
    private String salutation;

    @Pattern(regexp = UserServiceConstants.PATTERN_NAME)
    @Size(max = 50)
    private String name;

    @Pattern(regexp = UserServiceConstants.PATTERN_GENDER)
    @Size(max = 15)
    private String gender;

    @Pattern(regexp = UserServiceConstants.PATTERN_MOBILE)
    private String mobileNumber;
    
    @Pattern(regexp = UserServiceConstants.PATTERN_MOBILE)
    @JsonAlias({"alternateMobileNumber", "alternateMobileNo"})
    private String alternatemobilenumber;

    public String getAlternatemobilenumber() {
        return this.alternatemobilenumber;
    }

    public void setAlternatemobilenumber(String alternatemobilenumber) {
        this.alternatemobilenumber = alternatemobilenumber;
    }

    public String getAlternateMobileNumber() {
        return this.alternatemobilenumber;
    }

    public void setAlternateMobileNumber(String alternateMobileNumber) {
        this.alternatemobilenumber = alternateMobileNumber;
    }

    public String getGuardian() {
        return this.fatherOrHusbandName;
    }

    public void setGuardian(String guardian) {
        this.fatherOrHusbandName = guardian;
    }

    public String getGuardianRelation() {
        return this.relationship != null ? this.relationship.name() : null;
    }

    @Email
    @Size(max = 128)
    private String emailId;

    @Size(max = 50)
    private String altContactNumber;

    @Size(max = 10)
    private String pan;

    @Size(max = 20)
    private String aadhaarNumber;

    @JsonIgnore
    @Size(max = 300)
    private String permanentAddress;

    @JsonIgnore
    @Pattern(regexp = UserServiceConstants.PATTERN_CITY)
    @Size(max = 50)
    private String permanentCity;

    @JsonIgnore
    @Pattern(regexp = UserServiceConstants.PATTERN_PINCODE)
    @Size(max = 10)
    private String permanentPinCode;

    @JsonIgnore
    @Size(max = 300)
    private String correspondenceAddress;

    @JsonIgnore
    @Pattern(regexp = UserServiceConstants.PATTERN_CITY)
    @Size(max = 50)
    private String correspondenceCity;

    @JsonIgnore
    @Pattern(regexp = UserServiceConstants.PATTERN_PINCODE)
    @Size(max = 10)
    private String correspondencePinCode;
    private Boolean active;

    @Size(max = 16)
    private String locale;

    private UserType type;
    private Boolean accountLocked;
    private Long accountLockedDate;

    @Pattern(regexp = UserServiceConstants.PATTERN_NAME)
    @Size(max = 50)
    @JsonProperty("fatherOrHusbandName")
    @JsonAlias({"guardian"})
    private String fatherOrHusbandName;

    @JsonProperty("relationship")
    @JsonAlias({"guardianRelation"})
    private GuardianRelation relationship;

    @Size(max = 300)
    private String signature;

    @Size(max = 32)
    private String bloodGroup;

    @Size(max = 1000)
    private String photo;

    @Size(max = 300)
    private String identificationMark;
    private Long createdBy;

    @Size(max = 64)
    private String password;

    private String otpReference;
    private Long lastModifiedBy;

    @Pattern(regexp = UserServiceConstants.PATTERN_TENANT)
    @Size(max = 50)
    private String tenantId;

    private Set<RoleRequest> roles;

    @Size(max = 36)
    private String uuid;

    @Size(max = 36)
    private String digilockerid;

    @Pattern(regexp = UserServiceConstants.PATTERN_NAME)
    @Size(max = 100)
    private String fatherName;

    @Pattern(regexp = UserServiceConstants.PATTERN_NAME)
    @Size(max = 100)
    private String motherName;

    @Pattern(regexp = UserServiceConstants.PATTERN_NAME)
    @Size(max = 100)
    private String spouseName;

    private Boolean isIdentityVerified;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private Date identityVerifiedDate;

    private String identityVerifiedSource;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private Date lastResyncDate;

    private Integer edittedCount;
    private String lastLoginMethod;


    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private Date createdDate;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private Date lastModifiedDate;
    @JsonFormat(pattern = "dd/MM/yyyy")
    @JsonDeserialize(using = MultiFormatDateDeserializer.class)
    private Date dob;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private Date pwdExpiryDate;

    /**
     * Converts a domain {@link User} entity into a {@link UserRequest} contract DTO.
     *
     * @param user source domain user entity
     */
    public UserRequest(User user) {

        this.id = user.getId();
        this.access_token = user.getAccess_token();
        this.scope = user.getScope();
        this.userName = user.getUsername();
        this.salutation = user.getSalutation();
        this.name = user.getName();
        this.gender = user.getGender() != null ? user.getGender().toString() : null;
        this.mobileNumber = user.getMobileNumber();
        this.emailId = user.getEmailId();
        this.altContactNumber = user.getAltContactNumber();
        this.pan = user.getPan();
        this.aadhaarNumber = user.getAadhaarNumber();
        this.active = user.getActive();
        this.dob = user.getDob();
        this.pwdExpiryDate = user.getPasswordExpiryDate();
        this.locale = user.getLocale();
        this.type = user.getType();
        this.accountLocked = user.getAccountLocked();
        this.accountLockedDate = user.getAccountLockedDate();
        this.signature = user.getSignature();
        this.bloodGroup = user.getBloodGroup() != null ? user.getBloodGroup().getValue() : null;
        this.photo = user.getPhoto();
        this.identificationMark = user.getIdentificationMark();
        this.createdBy = user.getCreatedBy();
        this.createdDate = user.getCreatedDate();
        this.lastModifiedBy = user.getLastModifiedBy();
        this.lastModifiedDate = user.getLastModifiedDate();
        this.tenantId = user.getTenantId();
        this.roles = convertDomainRoleToContract(user.getRoles());
        this.fatherOrHusbandName = user.getGuardian();
        this.relationship = user.getGuardianRelation();
        this.uuid = user.getUuid();
        this.digilockerid = user.getDigilockerid();
        this.alternatemobilenumber=user.getAlternateMobileNumber();
        this.fatherName = user.getFatherName();
        this.motherName = user.getMotherName();
        this.spouseName = user.getSpouseName();
        this.isIdentityVerified = user.getIsIdentityVerified();
        this.identityVerifiedDate = user.getIdentityVerifiedDate();
        this.identityVerifiedSource = user.getIdentityVerifiedSource();
        this.lastResyncDate = user.getLastResyncDate();
        this.edittedCount = user.getEdittedCount();
        this.lastLoginMethod = user.getLastLoginMethod();
    }

    private void mapCorrespondenceAddress(User user) {
        if (user.getCorrespondenceAddress() != null) {
            this.correspondenceAddress = user.getCorrespondenceAddress().getAddress();
            this.correspondenceCity = user.getCorrespondenceAddress().getCity();
            this.correspondencePinCode = user.getCorrespondenceAddress().getPinCode();
        }
    }

    private void mapPermanentAddress(User user) {
        if (user.getPermanentAddress() != null) {
            this.permanentAddress = user.getPermanentAddress().getAddress();
            this.permanentCity = user.getPermanentAddress().getCity();
            this.permanentPinCode = user.getPermanentAddress().getPinCode();
        }
    }

    private Set<RoleRequest> convertDomainRoleToContract(Set<Role> roleEntities) {
        if (roleEntities == null) return new HashSet<>();
        return roleEntities.stream().map(RoleRequest::new).collect(Collectors.toSet());
    }

    /**
     * Converts this {@link UserRequest} DTO to a domain {@link User} entity.
     *
     * @param loggedInUserId   numeric ID of authenticated requester
     * @param loggedInUserUuid UUID of authenticated requester
     * @param isCreate         flag indicating create mode vs update mode
     * @return populated {@link User} domain entity
     */
    @JsonIgnore
    /**
     * Converts this contract DTO to a domain {@link User} model with operator audit metadata.
     *
     * @param loggedInUserId   ID of the operating user
     * @param loggedInUserUuid UUID of the operating user
     * @param isCreate         flag indicating if this is for a new user creation
     * @return populated domain {@link User} entity
     */
    public User toDomain(Long loggedInUserId, String loggedInUserUuid, boolean isCreate) {
        BloodGroup bloodGroup = null;
        try {
            if (this.bloodGroup != null)
                bloodGroup = BloodGroup.valueOf(this.bloodGroup.toUpperCase());
        } catch (Exception e) {
            bloodGroup = BloodGroup.fromValue(this.bloodGroup);
        }
        return User.builder()
                .uuid(this.uuid)
                .scope(this.scope)
                .id(this.id)
                .name(this.name)
                .username(this.userName)
                .salutation(this.salutation)
                .mobileNumber(this.mobileNumber)
                .emailId(this.emailId)
                .altContactNumber(this.altContactNumber)
                .pan(this.pan)
                .aadhaarNumber(this.aadhaarNumber)
                .digilockerid(this.digilockerid)
                .active(isActive(isCreate))
                .dob(this.dob)
                .passwordExpiryDate(this.pwdExpiryDate)
                .locale(this.locale)
                .type(this.type)
                .accountLocked(isAccountLocked(isCreate))
                .accountLockedDate(this.accountLockedDate)
                .signature(this.signature)
                .photo(this.photo)
                .identificationMark(this.identificationMark)
                .gender(this.gender != null ? Gender.valueOf(this.gender.toUpperCase()) : null)
                .bloodGroup(bloodGroup)
                .lastModifiedDate(new Date())
                .createdDate(new Date())
                .otpReference(this.otpReference)
                .tenantId(this.tenantId)
                .password(this.password)
                .roles(toDomainRoles())
                .loggedInUserId(loggedInUserId)
                .loggedInUserUuid(loggedInUserUuid)
                .permanentAddress(null)
                .correspondenceAddress(null)
                .guardian(fatherOrHusbandName)
                .guardianRelation(relationship).alternateMobileNumber(this.alternatemobilenumber)
                .fatherName(this.fatherName)
                .motherName(this.motherName)
                .spouseName(this.spouseName)
                .isIdentityVerified(isCreate ? Boolean.FALSE : null)
                .identityVerifiedDate(null)
                .identityVerifiedSource(null)
                .lastResyncDate(this.lastResyncDate)
                .edittedCount(this.edittedCount)
                .lastLoginMethod(this.lastLoginMethod)
                .build();
    }

    private Boolean isActive(boolean isCreate) {
        if (this.active == null && isCreate) {
            return false;
        }
        return this.active;
    }

    private Boolean isAccountLocked(boolean isCreate) {
        if (this.accountLocked == null && isCreate) {
            return false;
        }
        return this.accountLocked;
    }

    private Address toDomainPermanentAddress() {
        return Address.builder()
                .type(AddressType.PERMANENT)
                .city(permanentCity)
                .pinCode(permanentPinCode)
                .address(permanentAddress)
                .build();
    }

    private Address toDomainCorrespondenceAddress() {
        return Address.builder()
                .type(AddressType.CORRESPONDENCE)
                .city(correspondenceCity)
                .pinCode(correspondencePinCode)
                .address(correspondenceAddress)
                .build();
    }

    private Set<Role> toDomainRoles() {
        // CRITICAL FIX: Return empty HashSet instead of null to prevent gateway authorization failures
        return this.roles != null
                ? this.roles.stream()
                .map(RoleRequest::toDomain)
                .distinct()
                .collect(Collectors.toSet())
                : new HashSet<>();
    }
}
