package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.config.UserServiceConstants;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.UserDocument;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.egov.user.web.contract.deserializer.MultiFormatDateDeserializer;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Request payload for creating a complete citizen profile with all Section 8 mandatory fields.
 *
 * <p>Enforces:</p>
 * <ul>
 *   <li>Mandatory Demographics: Name, Father's Name, Mother's Name, Mobile Number, DOB, Email ID, Gender</li>
 *   <li>Optional Demographics: Spouse Name, Guardian Name, Alternate Mobile Number, Profile Picture</li>
 *   <li>Mandatory Address: City, Locality, Street Name, House Number, Landmark, Pin Code, Address Type</li>
 *   <li>Mandatory Documents: Address Proof, Identity Proof, D.O.B. Proof</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 2.9.0
 */
@Schema(description = "Request envelope for creating a complete citizen profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCreateRequest {

    @Schema(description = "Standard UPYOG RequestInfo contract")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @Schema(description = "Tenant identifier", example = "pg.citya", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "tenantId is mandatory for profile creation")
    @Pattern(regexp = UserServiceConstants.PATTERN_TENANT, message = "Invalid tenantId format")
    @Size(max = 50)
    private String tenantId;

    @Schema(description = "Citizen full name (Mandatory)", example = "Aarav Sharma", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is mandatory for profile creation")
    @Size(max = 100)
    private String name;

    @Schema(description = "Father's full name (Mandatory)", example = "Rajesh Sharma", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Father's Name is mandatory for profile creation")
    @Size(max = 100)
    private String fatherName;

    @Schema(description = "Mother's full name (Mandatory)", example = "Sunita Sharma", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Mother's Name is mandatory for profile creation")
    @Size(max = 100)
    private String motherName;

    @Schema(description = "Spouse full name (Optional)", example = "Pooja Sharma")
    @Size(max = 100)
    private String spouseName;

    @Schema(description = "Guardian full name (Mandatory for minors under 18)", example = "Rajesh Sharma")
    @JsonAlias({"guardianName", "guardian_name"})
    @Size(max = 100)
    private String guardian;

    @Schema(description = "Guardian relationship: FATHER, MOTHER, HUSBAND, OTHER (Mandatory for minors under 18)", example = "FATHER")
    @JsonAlias({"guardianRelationship", "guardian_relation", "guardian_relationship"})
    @Size(max = 32)
    private String guardianRelation;

    @Schema(description = "Citizen 10-digit mobile number (Mandatory)", example = "9876543210", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Mobile number is mandatory for profile creation")
    @Pattern(regexp = UserServiceConstants.PATTERN_MOBILE, message = "Invalid mobile number format")
    private String mobileNumber;

    @Schema(description = "Alternate mobile number (Optional)", example = "9123456789")
    @Pattern(regexp = UserServiceConstants.PATTERN_MOBILE, message = "Invalid alternate mobile number format")
    private String alternateMobileNumber;

    @Schema(description = "Date of Birth (Mandatory, dd/MM/yyyy or yyyy-MM-dd)", example = "15/08/1990", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Date of Birth is mandatory for profile creation")
    @JsonDeserialize(using = MultiFormatDateDeserializer.class)
    @JsonFormat(pattern = "dd/MM/yyyy")
    private Date dob;

    @Schema(description = "Citizen email ID (Mandatory)", example = "aarav.sharma@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email ID is mandatory for profile creation")
    @Email(message = "Invalid emailId format")
    @Size(max = 128)
    private String emailId;

    @Schema(description = "Citizen gender (Mandatory: MALE, FEMALE, OTHERS, TRANSGENDER)", example = "MALE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Gender is mandatory for profile creation")
    private String gender;

    @Schema(description = "Upload profile picture URL or filestore ID (Optional)")
    private String photo;

    @Schema(description = "Citizen 12-digit Aadhaar number (Optional)", example = "XXXXXXXX1234")
    private String aadhaarNumber;

    @Schema(description = "Citizen address details (Mandatory)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Address details are mandatory for profile creation")
    @Valid
    private Address address;

    @Schema(description = "List of citizen addresses with individual address proofs")
    @JsonProperty("addresses")
    private List<Address> addresses;

    @Schema(description = "List of attached document proofs (Mandatory: Address Proof, Identity Proof, D.O.B. Proof)")
    private List<UserDocument> documents;

    @Schema(description = "Upload Address Proof filestore ID (Mandatory if not in documents list or address object)", example = "filestore-addr-1234")
    private String addressProofFileStoreId;

    @Schema(description = "Upload Identity Proof filestore ID (Mandatory if not in documents list)", example = "filestore-ident-1234")
    private String identityProofFileStoreId;

    @Schema(description = "Upload D.O.B. Proof filestore ID (Mandatory if not in documents list)", example = "filestore-dob-1234")
    private String dobProofFileStoreId;

    @Schema(description = "Nested citizen profile payload supporting standard UPYOG request contracts")
    @JsonProperty("user")
    @JsonAlias({"profile", "User"})
    private Map<String, Object> user;

    public String getTenantId() {
        if (this.tenantId != null && !this.tenantId.trim().isEmpty()) {
            return this.tenantId;
        }
        if (this.user != null && this.user.get("tenantId") != null) {
            return String.valueOf(this.user.get("tenantId"));
        }
        return this.tenantId;
    }

    @JsonProperty("addresses")
    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
        if (addresses != null && !addresses.isEmpty() && this.address == null) {
            this.address = addresses.get(0);
        }
    }

    public List<Address> getEffectiveAddresses() {
        if (this.addresses != null && !this.addresses.isEmpty()) {
            return this.addresses;
        }
        if (this.address != null) {
            return Collections.singletonList(this.address);
        }
        return Collections.emptyList();
    }

    @JsonProperty("user")
    @JsonAlias({"profile", "User"})
    public Map<String, Object> getUser() {
        return this.user;
    }

    @JsonSetter("user")
    @JsonAlias({"profile", "User"})
    public void setUser(Map<String, Object> userMap) {
        this.user = userMap;
        unpackUserIfPresent();
    }

    public void unpackUserIfPresent() {
        if (this.user == null) return;
        Map<String, Object> userMap = this.user;
        if ((this.tenantId == null || this.tenantId.trim().isEmpty()) && userMap.get("tenantId") != null) {
            this.tenantId = String.valueOf(userMap.get("tenantId"));
        }
        if ((this.name == null || this.name.trim().isEmpty()) && userMap.get("name") != null) {
            this.name = String.valueOf(userMap.get("name"));
        }
        if ((this.fatherName == null || this.fatherName.trim().isEmpty()) && userMap.get("fatherName") != null) {
            this.fatherName = String.valueOf(userMap.get("fatherName"));
        }
        if ((this.motherName == null || this.motherName.trim().isEmpty()) && userMap.get("motherName") != null) {
            this.motherName = String.valueOf(userMap.get("motherName"));
        }
        if ((this.spouseName == null || this.spouseName.trim().isEmpty()) && userMap.get("spouseName") != null) {
            this.spouseName = String.valueOf(userMap.get("spouseName"));
        }
        if (this.guardian == null || this.guardian.trim().isEmpty()) {
            if (userMap.get("guardian") != null) this.guardian = String.valueOf(userMap.get("guardian"));
            else if (userMap.get("guardianName") != null) this.guardian = String.valueOf(userMap.get("guardianName"));
            else if (userMap.get("fatherOrHusbandName") != null) this.guardian = String.valueOf(userMap.get("fatherOrHusbandName"));
        }
        if (this.guardianRelation == null || this.guardianRelation.trim().isEmpty()) {
            if (userMap.get("guardianRelation") != null) this.guardianRelation = String.valueOf(userMap.get("guardianRelation"));
            else if (userMap.get("guardianRelationship") != null) this.guardianRelation = String.valueOf(userMap.get("guardianRelationship"));
            else if (userMap.get("relationship") != null) this.guardianRelation = String.valueOf(userMap.get("relationship"));
        }
        if ((this.mobileNumber == null || this.mobileNumber.trim().isEmpty()) && userMap.get("mobileNumber") != null) {
            this.mobileNumber = String.valueOf(userMap.get("mobileNumber"));
        }
        if (this.alternateMobileNumber == null || this.alternateMobileNumber.trim().isEmpty()) {
            if (userMap.get("alternateMobileNumber") != null) this.alternateMobileNumber = String.valueOf(userMap.get("alternateMobileNumber"));
            else if (userMap.get("alternatemobilenumber") != null) this.alternateMobileNumber = String.valueOf(userMap.get("alternatemobilenumber"));
        }
        if ((this.emailId == null || this.emailId.trim().isEmpty()) && userMap.get("emailId") != null) {
            this.emailId = String.valueOf(userMap.get("emailId"));
        }
        if ((this.gender == null || this.gender.trim().isEmpty()) && userMap.get("gender") != null) {
            this.gender = String.valueOf(userMap.get("gender"));
        }
        if ((this.photo == null || this.photo.trim().isEmpty()) && userMap.get("photo") != null) {
            this.photo = String.valueOf(userMap.get("photo"));
        }
        if ((this.aadhaarNumber == null || this.aadhaarNumber.trim().isEmpty()) && userMap.get("aadhaarNumber") != null) {
            this.aadhaarNumber = String.valueOf(userMap.get("aadhaarNumber"));
        }
        if (this.dob == null && userMap.get("dob") != null) {
            this.dob = parseDateHelper(userMap.get("dob"));
        }
        ObjectMapper mapper = new ObjectMapper();
        if (this.address == null && userMap.containsKey("address")) {
            try {
                this.address = mapper.convertValue(userMap.get("address"), Address.class);
            } catch (Exception ignored) {}
        }
        if (userMap.containsKey("addresses")) {
            try {
                TypeReference<List<Address>> addrListRef = new TypeReference<List<Address>>() {};
                List<Address> addrs = mapper.convertValue(userMap.get("addresses"), addrListRef);
                if (addrs != null && !addrs.isEmpty()) {
                    this.addresses = addrs;
                    if (this.address == null) {
                        this.address = addrs.get(0);
                    }
                }
            } catch (Exception ignored) {}
        }
        if ((this.addressProofFileStoreId == null || this.addressProofFileStoreId.trim().isEmpty())) {
            if (this.address != null && this.address.getFilestoreId() != null && !this.address.getFilestoreId().trim().isEmpty()) {
                this.addressProofFileStoreId = this.address.getFilestoreId();
            } else if (this.addresses != null && !this.addresses.isEmpty() && this.addresses.get(0).getFilestoreId() != null && !this.addresses.get(0).getFilestoreId().trim().isEmpty()) {
                this.addressProofFileStoreId = this.addresses.get(0).getFilestoreId();
            }
        }
        if ((this.documents == null || this.documents.isEmpty()) && userMap.containsKey("documents")) {
            try {
                TypeReference<List<UserDocument>> typeRef = new TypeReference<List<UserDocument>>() {};
                this.documents = mapper.convertValue(userMap.get("documents"), typeRef);
            } catch (Exception ignored) {}
        }
        if ((this.addressProofFileStoreId == null || this.addressProofFileStoreId.trim().isEmpty()) && userMap.get("addressProofFileStoreId") != null) {
            this.addressProofFileStoreId = String.valueOf(userMap.get("addressProofFileStoreId"));
        }
        if ((this.identityProofFileStoreId == null || this.identityProofFileStoreId.trim().isEmpty()) && userMap.get("identityProofFileStoreId") != null) {
            this.identityProofFileStoreId = String.valueOf(userMap.get("identityProofFileStoreId"));
        }
        if ((this.dobProofFileStoreId == null || this.dobProofFileStoreId.trim().isEmpty()) && userMap.get("dobProofFileStoreId") != null) {
            this.dobProofFileStoreId = String.valueOf(userMap.get("dobProofFileStoreId"));
        }
    }

    private Date parseDateHelper(Object dobObj) {
        if (dobObj == null) return null;
        if (dobObj instanceof Number) {
            return new Date(((Number) dobObj).longValue());
        }
        String str = String.valueOf(dobObj).trim();
        if (str.isEmpty()) return null;
        if (str.matches("^\\d+$")) {
            try {
                return new Date(Long.parseLong(str));
            } catch (Exception ignored) {}
        }
        String[] patterns = {"yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'"};
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern);
                sdf.setLenient(false);
                return sdf.parse(str);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
