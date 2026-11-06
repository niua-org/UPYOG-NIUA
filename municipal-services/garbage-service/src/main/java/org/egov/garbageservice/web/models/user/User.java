package org.egov.garbageservice.web.models.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.egov.common.contract.request.Role;
import org.egov.tracer.annotations.CustomSafeHtml;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Validated
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class User {

    @JsonProperty("id")
    private Long id;

    @Size(max = 64)
    @CustomSafeHtml
    @JsonProperty("uuid")
    private String uuid;

    @Size(max = 64)
    @CustomSafeHtml
    @JsonProperty("userName")
    private String userName;

    @Size(max = 64)
    @CustomSafeHtml
    @JsonProperty("password")
    private String password;

    @JsonProperty("salutation")
    @CustomSafeHtml
    private String salutation;

    @NotNull
    @CustomSafeHtml
    @Size(max = 100)
    @JsonProperty("name")
    private String name;

    @CustomSafeHtml
    @JsonProperty("gender")
    private String gender;

    @NotNull
    @CustomSafeHtml
    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @Size(max = 128)
    @CustomSafeHtml
    @JsonProperty("emailId")
    private String emailId;

    @Size(max = 50)
    @CustomSafeHtml
    @JsonProperty("altContactNumber")
    private String altContactNumber;

    @Size(max = 10)
    @CustomSafeHtml
    @JsonProperty("pan")
    private String pan;

    @CustomSafeHtml
    @Pattern(regexp = "^[0-9]{12}$", message = "AadhaarNumber should be 12 digit number")
    @JsonProperty("aadhaarNumber")
    private String aadhaarNumber;

    @Size(max = 300)
    @CustomSafeHtml
    @JsonProperty("permanentAddress")
    private String permanentAddress;

    @Size(max = 300)
    @CustomSafeHtml
    @JsonProperty("permanentCity")
    private String permanentCity;

    @Size(max = 10)
    @CustomSafeHtml
    @JsonProperty("permanentPinCode")
    private String permanentPincode;

    @Size(max = 300)
    @CustomSafeHtml
    @JsonProperty("correspondenceCity")
    private String correspondenceCity;

    @Size(max = 10)
    @CustomSafeHtml
    @JsonProperty("correspondencePinCode")
    private String correspondencePincode;

    @Size(max = 300)
    @CustomSafeHtml
    @JsonProperty("correspondenceAddress")
    private String correspondenceAddress;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("dob")
    private Long dob;

    @JsonProperty("pwdExpiryDate")
    private Long pwdExpiryDate;

    @Size(max = 16)
    @CustomSafeHtml
    @JsonProperty("locale")
    private String locale;

    @Size(max = 50)
    @CustomSafeHtml
    @JsonProperty("type")
    private String type;

    @Size(max = 36)
    @CustomSafeHtml
    @JsonProperty("signature")
    private String signature;

    @JsonProperty("accountLocked")
    private Boolean accountLocked;

    @JsonProperty("roles")
    @Valid
    private List<Role> roles;

    @Size(max = 100)
    @CustomSafeHtml
    @JsonProperty("fatherOrHusbandName")
    private String fatherOrHusbandName;

    @Size(max = 32)
    @CustomSafeHtml
    @JsonProperty("bloodGroup")
    private String bloodGroup;

    @Size(max = 300)
    @CustomSafeHtml
    @JsonProperty("identificationMark")
    private String identificationMark;

    @Size(max = 36)
    @CustomSafeHtml
    @JsonProperty("photo")
    private String photo;

    @Size(max = 64)
    @CustomSafeHtml
    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdDate")
    private Long createdDate;

    @Size(max = 64)
    @CustomSafeHtml
    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy;

    @JsonProperty("lastModifiedDate")
    private Long lastModifiedDate;

    @Size(max = 256)
    @CustomSafeHtml
    @JsonProperty("tenantId")
    private String tenantId;

    @Size(max = 50)
    @CustomSafeHtml
    @JsonProperty("alternatemobilenumber")
    private String alternatemobilenumber;

    @JsonProperty("addresses")
    private List<AddressV2> addresses;

    public User addAddressItem(AddressV2 addressItem) {
        if (this.addresses == null) {
            this.addresses = new ArrayList<>();
        }
        this.addresses.add(addressItem);
        return this;
    }

    public User addRolesItem(Role rolesItem) {
        if (this.roles == null) {
            this.roles = new ArrayList<>();
        }
        this.roles.add(rolesItem);
        return this;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getSalutation() { return salutation; }
    public void setSalutation(String salutation) { this.salutation = salutation; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getEmailId() { return emailId; }
    public void setEmailId(String emailId) { this.emailId = emailId; }

    public String getAltContactNumber() { return altContactNumber; }
    public void setAltContactNumber(String altContactNumber) { this.altContactNumber = altContactNumber; }

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public List<Role> getRoles() { return roles; }
    public void setRoles(List<Role> roles) { this.roles = roles; }

    public String getFatherOrHusbandName() { return fatherOrHusbandName; }
    public void setFatherOrHusbandName(String fatherOrHusbandName) { this.fatherOrHusbandName = fatherOrHusbandName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(uuid, user.uuid) &&
                Objects.equals(name, user.name) &&
                Objects.equals(mobileNumber, user.mobileNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name, mobileNumber);
    }
}
