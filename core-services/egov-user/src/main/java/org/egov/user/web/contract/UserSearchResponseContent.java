package org.egov.user.web.contract;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.enums.BloodGroup;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.model.enums.GuardianRelation;
import org.egov.user.domain.model.enums.UserType;

import org.egov.user.web.contract.profile.ProfileIncludeEntity;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Response DTO representing an individual user item returned by search endpoints.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class UserSearchResponseContent {

    private Long id;
    private String userName;
    private String salutation;
    private String name;
    private String gender;
    private String mobileNumber;
    private String emailId;
    private String altContactNumber;
    private String pan;
    private String aadhaarNumber;

    @JsonIgnore
    private String permanentAddress;
    @JsonIgnore
    private String permanentCity;
    @JsonIgnore
    private String permanentPinCode;
    @JsonIgnore
    private String correspondenceAddress;
    @JsonIgnore
    private String correspondenceCity;
    @JsonIgnore
    private String correspondencePinCode;
    private String alternatemobilenumber;

    @JsonProperty("addresses")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<Address> addresses;

    @JsonProperty("documents")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<UserDocument> documents;

    private Boolean active;
    private String locale;
    private UserType type;
    private Boolean accountLocked;
    private Long accountLockedDate;
    private String fatherOrHusbandName;
    private GuardianRelation relationship;
    private String signature;
    private String bloodGroup;
    private String photo;
    private String identificationMark;
    private Long createdBy;
    private Long lastModifiedBy;
    private String tenantId;
    private Set<RoleRequest> roles;
    private String uuid;
    private String digilockerid;
    private String fatherName;
    private String motherName;
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
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date dob;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private Date pwdExpiryDate;

    /**
     * Legacy constructor for core search endpoints ({@code /_search}, {@code /v1/_search}, {@code /users/v2/_search}).
     * <p>
     * Populates core identity, audit, and demographic profile fields.
     * Crucially, {@code addresses} and {@code documents} collections are <b>NOT</b> populated and remain {@code null}
     * to preserve contract compatibility and prevent PII exposure for legacy and v2 callers
     * (e.g. PT, TL, PGR, HRMS, and UI).
     *
     * @param user the domain {@link User} entity to copy fields from
     */
    /**
     * Legacy constructor used by existing user search callers (e.g. {@code /_search},
     * {@code /v1/_search}, and {@code /users/v2/_search} consumed by PT, TL, PGR, and HRMS).
     * <p>
     * Maps core identity, audit, and demographic profile fields while omitting nested addresses
     * and documents to prevent PII exposure and preserve contract compatibility.
     *
     * @param user the domain {@link User} entity to convert
     */
    public UserSearchResponseContent(User user) {
        this(user, Collections.singleton(ProfileIncludeEntity.PROFILE));
    }

    /**
     * Modular constructor supporting section-based inclusion for citizen profile searches ({@code /profile/_search}).
     * <p>
     * <b>Included field mappings by section:</b>
     * <ul>
     *   <li><b>Core Identity & Audit (Always Mapped regardless of includes):</b>
     *     <ul>
     *       <li>Identity: {@code id}, {@code uuid}, {@code userName}, {@code tenantId}, {@code type}, {@code active}</li>
     *       <li>Audit: {@code createdBy}, {@code createdDate}, {@code lastModifiedBy}, {@code lastModifiedDate}</li>
     *     </ul>
     *   </li>
     *   <li><b>{@link ProfileIncludeEntity#PROFILE}:</b>
     *     Maps citizen personal details, demographics, roles, and verification status:
     *     {@code salutation}, {@code name}, {@code gender}, {@code mobileNumber}, {@code emailId},
     *     {@code altContactNumber}, {@code pan}, {@code aadhaarNumber}, {@code dob}, {@code pwdExpiryDate},
     *     {@code locale}, {@code accountLocked}, {@code accountLockedDate}, {@code signature}, {@code bloodGroup},
     *     {@code photo}, {@code identificationMark}, {@code roles}, {@code fatherOrHusbandName}, {@code relationship},
     *     {@code digilockerid}, {@code alternatemobilenumber}, {@code fatherName}, {@code motherName}, {@code spouseName},
     *     {@code isIdentityVerified}, {@code identityVerifiedDate}, {@code identityVerifiedSource}, {@code lastResyncDate},
     *     {@code edittedCount}, and {@code lastLoginMethod}.
     *   </li>
     *   <li><b>{@link ProfileIncludeEntity#ADDRESS}:</b>
     *     Maps the collection of active address objects:
     *     {@code addresses} ({@link Address} sets including address lines, city, pincode, type, status, and filestore IDs).
     *     Omitted (null) if {@link ProfileIncludeEntity#ADDRESS} is not requested.
     *   </li>
     *   <li><b>{@link ProfileIncludeEntity#DOCUMENT}:</b>
     *     Maps the collection of active user documents:
     *     {@code documents} ({@link UserDocument} list including document type, filestore ID, file download URL, and verification status).
     *     Omitted (null) if {@link ProfileIncludeEntity#DOCUMENT} is not requested.
     *   </li>
     * </ul>
     *
     * @param user the domain {@link User} entity
     * @param includes set of {@link ProfileIncludeEntity} sections specifying which entity blocks to include
     */
    public UserSearchResponseContent(User user, Set<ProfileIncludeEntity> includes) {
        if (user == null) return;

        // Core Identity & Audit Fields (always present)
        this.id = user.getId();
        this.uuid = user.getUuid();
        this.userName = user.getUsername();
        this.tenantId = user.getTenantId();
        this.type = user.getType();
        this.active = user.getActive();
        this.createdBy = user.getCreatedBy();
        this.createdDate = user.getCreatedDate();
        this.lastModifiedBy = user.getLastModifiedBy();
        this.lastModifiedDate = user.getLastModifiedDate();

        boolean includeProfile = includes != null && includes.contains(ProfileIncludeEntity.PROFILE);

        if (includeProfile) {
            this.salutation = user.getSalutation();
            this.name = user.getName();
            this.gender = user.getGender() != null ? user.getGender().toString() : null;
            this.mobileNumber = user.getMobileNumber();
            this.emailId = user.getEmailId();
            this.altContactNumber = user.getAltContactNumber();
            this.pan = user.getPan();
            this.aadhaarNumber = user.getAadhaarNumber();
            this.dob = user.getDob();
            this.pwdExpiryDate = user.getPasswordExpiryDate();
            this.locale = user.getLocale();
            this.accountLocked = user.getAccountLocked();
            this.accountLockedDate = user.getAccountLockedDate();
            this.signature = user.getSignature();
            this.bloodGroup = user.getBloodGroup() != null ? user.getBloodGroup().getValue() : null;
            this.photo = user.getPhoto();
            this.identificationMark = user.getIdentificationMark();
            this.roles = convertDomainRolesToContract(user.getRoles());
            log.info("UserSearchResponseContent - User {} (UUID: {}) converted {} roles to contract",
                    user.getId(), user.getUuid(), this.roles != null ? this.roles.size() : 0);
            this.fatherOrHusbandName = user.getGuardian();
            this.relationship = user.getGuardianRelation();
            this.digilockerid = user.getDigilockerid();
            this.alternatemobilenumber = user.getAlternateMobileNumber();
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

        if (includes != null && includes.contains(ProfileIncludeEntity.ADDRESS)) {
            this.addresses = user.getAddresses();
        }

        if (includes != null && includes.contains(ProfileIncludeEntity.DOCUMENT)) {
            this.documents = user.getDocuments();
        }
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


    private Set<RoleRequest> convertDomainRolesToContract(Set<Role> roleEntities) {
        if (roleEntities == null) return new HashSet<>();
        return roleEntities.stream().map(RoleRequest::new).collect(Collectors.toSet());
    }

    
    /**
     * Converts this {@link UserSearchResponseContent} DTO back to a domain {@link User} entity.
     *
     * @return reconstructed {@link User} entity
     */
    public User toUser() {
    	 
        User user = new User();
 
        user.setId(this.id);
        user.setUuid(this.uuid);
        user.setDigilockerid(this.digilockerid);
        user.setTenantId(this.tenantId);
        user.setUsername(this.userName);
        user.setSalutation(this.salutation);
        user.setName(this.name);
        user.setMobileNumber(this.mobileNumber);
        user.setEmailId(this.emailId);
        user.setAltContactNumber(this.altContactNumber);
        user.setPan(this.pan);
        user.setAadhaarNumber(this.aadhaarNumber);
 
        // Gender conversion
        if (this.gender != null) {
            try {
                user.setGender(Gender.valueOf(this.gender.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid gender value: " + this.gender);
            }
        }
 
        // BloodGroup conversion
        if (this.bloodGroup != null) {
            try {
                user.setBloodGroup(BloodGroup.valueOf(this.bloodGroup.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid blood group value: " + this.bloodGroup);
            }
        }
 
        user.setLocale(this.locale);
        user.setType(this.type);
        user.setAccountLocked(this.accountLocked);
        user.setAccountLockedDate(this.accountLockedDate);
        user.setDob(this.dob);
        user.setPasswordExpiryDate(this.pwdExpiryDate);
        user.setSignature(this.signature);
        user.setPhoto(this.photo);
        user.setIdentificationMark(this.identificationMark);
        user.setCreatedBy(this.createdBy);
        user.setCreatedDate(this.createdDate);
        user.setLastModifiedBy(this.lastModifiedBy);
        user.setLastModifiedDate(this.lastModifiedDate);
        user.setGuardian(this.fatherOrHusbandName);
        user.setGuardianRelation(this.relationship);
        user.setActive(this.active);
        user.setAlternateMobileNumber(this.alternatemobilenumber);
        user.setFatherName(this.fatherName);
        user.setMotherName(this.motherName);
        user.setSpouseName(this.spouseName);
        user.setIsIdentityVerified(this.isIdentityVerified);
        user.setIdentityVerifiedDate(this.identityVerifiedDate);
        user.setIdentityVerifiedSource(this.identityVerifiedSource);
        user.setLastResyncDate(this.lastResyncDate);
        user.setEdittedCount(this.edittedCount);
        user.setLastLoginMethod(this.lastLoginMethod);
 
        // Address mappings
        if (this.permanentAddress != null || this.permanentCity != null || this.permanentPinCode != null) {
            Address permanentAddress = new Address();
            permanentAddress.setAddress(this.permanentAddress);
            permanentAddress.setCity(this.permanentCity);
            permanentAddress.setPinCode(this.permanentPinCode);
            user.setPermanentAddress(permanentAddress);
        }
 
        if (this.correspondenceAddress != null || this.correspondenceCity != null || this.correspondencePinCode != null) {
            Address correspondenceAddress = new Address();
            correspondenceAddress.setAddress(this.correspondenceAddress);
            correspondenceAddress.setCity(this.correspondenceCity);
            correspondenceAddress.setPinCode(this.correspondencePinCode);
            user.setCorrespondenceAddress(correspondenceAddress);
        }
 
        user.setAddresses(this.addresses);
        user.setDocuments(this.documents);
 
        // Roles conversion
        if (this.roles != null) {
            Set<Role> roleEntities = this.roles.stream()
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
}
