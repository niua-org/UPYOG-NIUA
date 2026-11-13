package org.egov.user.web.contract.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.asset.UserIncome;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.domain.model.asset.UserVehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload detailing citizen profile demographics and asset declarations (properties, income, vehicles, documents)
 * for a family member synchronized from family service to {@code egov-user}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MemberAssetDetail {

    /**
     * Unique citizen user UUID in {@code eg_user}.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Tenant identifier scoping the synchronization.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Optional family unit business identifier.
     */
    @JsonProperty("familyId")
    private String familyId;

    /**
     * Nested citizen demographic profile details if provided in structured format.
     */
    @Valid
    @ToString.Exclude
    @JsonProperty("citizenDetails")
    private CitizenProfileDetail citizenDetails;

    // Direct / flat demographic attribute fallbacks (all PII excluded from toString)
    @ToString.Exclude
    @JsonProperty("name")
    private String name;

    @ToString.Exclude
    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @ToString.Exclude
    @JsonProperty("gender")
    private String gender;

    @ToString.Exclude
    @JsonProperty("dob")
    private Long dob;

    @ToString.Exclude
    @JsonProperty("photo")
    private String photo;

    @ToString.Exclude
    @JsonProperty("emailId")
    private String emailId;

    @ToString.Exclude
    @JsonProperty("alternateMobileNumber")
    private String alternateMobileNumber;

    @ToString.Exclude
    @JsonProperty("fatherName")
    private String fatherName;

    @ToString.Exclude
    @JsonProperty("motherName")
    private String motherName;

    @ToString.Exclude
    @JsonProperty("spouseName")
    private String spouseName;

    @ToString.Exclude
    @JsonProperty("guardian")
    private String guardian;

    @ToString.Exclude
    @JsonProperty("guardianRelation")
    private String guardianRelation;

    @ToString.Exclude
    @JsonProperty("caste")
    private String caste;

    @ToString.Exclude
    @JsonProperty("isSpeciallyAbled")
    private Boolean isSpeciallyAbled;

    /**
     * Attached citizen identity and proof documents (PII excluded from toString).
     */
    @Valid
    @ToString.Exclude
    @JsonProperty("documents")
    @Builder.Default
    private List<UserDocument> documents = new ArrayList<>();

    /**
     * Declared property assets (Rural and Urban) owned by this member (excluded from toString).
     */
    @Valid
    @ToString.Exclude
    @JsonProperty("properties")
    @Builder.Default
    private List<UserProperty> properties = new ArrayList<>();

    /**
     * Declared income and tax identifier details for this member (financial PII excluded from toString).
     */
    @Valid
    @ToString.Exclude
    @JsonProperty("income")
    private UserIncome income;

    /**
     * Declared vehicle ownerships for this member (asset details excluded from toString).
     */
    @Valid
    @ToString.Exclude
    @JsonProperty("vehicles")
    @Builder.Default
    private List<UserVehicle> vehicles = new ArrayList<>();

    /**
     * Resolves effective citizen UUID between direct {@code userUuid} and nested {@code citizenDetails.uuid}.
     *
     * @return citizen UUID string
     */
    public String resolveUserUuid() {
        if (StringUtils.isNotBlank(userUuid)) {
            return userUuid;
        }
        return citizenDetails != null ? citizenDetails.getUuid() : null;
    }

    /**
     * Resolves effective father name between direct and nested fields.
     *
     * @return father name string
     */
    public String resolveFatherName() {
        if (StringUtils.isNotBlank(fatherName)) return fatherName;
        return citizenDetails != null ? citizenDetails.getFatherName() : null;
    }

    /**
     * Resolves effective mother name between direct and nested fields.
     *
     * @return mother name string
     */
    public String resolveMotherName() {
        if (StringUtils.isNotBlank(motherName)) return motherName;
        return citizenDetails != null ? citizenDetails.getMotherName() : null;
    }

    /**
     * Resolves effective spouse name between direct and nested fields.
     *
     * @return spouse name string
     */
    public String resolveSpouseName() {
        if (StringUtils.isNotBlank(spouseName)) return spouseName;
        return citizenDetails != null ? citizenDetails.getSpouseName() : null;
    }

    /**
     * Resolves effective guardian name between direct and nested fields.
     *
     * @return guardian name string
     */
    public String resolveGuardian() {
        if (StringUtils.isNotBlank(guardian)) return guardian;
        return citizenDetails != null ? citizenDetails.getGuardian() : null;
    }

    /**
     * Resolves effective guardian relation between direct and nested fields.
     *
     * @return guardian relation string
     */
    public String resolveGuardianRelation() {
        if (StringUtils.isNotBlank(guardianRelation)) return guardianRelation;
        return citizenDetails != null ? citizenDetails.getGuardianRelation() : null;
    }

    /**
     * Resolves effective caste between direct and nested fields.
     *
     * @return caste code string
     */
    public String resolveCaste() {
        if (StringUtils.isNotBlank(caste)) return caste;
        return citizenDetails != null ? citizenDetails.getCaste() : null;
    }

    /**
     * Resolves effective specially abled indicator between direct and nested fields.
     *
     * @return specially abled boolean flag
     */
    public Boolean resolveIsSpeciallyAbled() {
        if (isSpeciallyAbled != null) return isSpeciallyAbled;
        return citizenDetails != null ? citizenDetails.getIsSpeciallyAbled() : null;
    }

    /**
     * Resolves effective alternate mobile number between direct and nested fields.
     *
     * @return alternate mobile string
     */
    public String resolveAlternateMobileNumber() {
        if (StringUtils.isNotBlank(alternateMobileNumber)) return alternateMobileNumber;
        return citizenDetails != null ? citizenDetails.getAlternateMobileNumber() : null;
    }

    /**
     * Resolves effective email address between direct and nested fields.
     *
     * @return email string
     */
    public String resolveEmailId() {
        if (StringUtils.isNotBlank(emailId)) return emailId;
        return citizenDetails != null ? citizenDetails.getEmailId() : null;
    }

    /**
     * Resolves effective photo FileStore ID/URL between direct and nested fields.
     *
     * @return photo string
     */
    public String resolvePhoto() {
        if (StringUtils.isNotBlank(photo)) return photo;
        return citizenDetails != null ? citizenDetails.getPhoto() : null;
    }

    /**
     * Resolves all attached documents combining top-level documents and nested citizen documents.
     *
     * @return combined list of {@link UserDocument}
     */
    public List<UserDocument> resolveAllDocuments() {
        List<UserDocument> combined = new ArrayList<>();
        if (documents != null) {
            combined.addAll(documents);
        }
        if (citizenDetails != null && citizenDetails.getDocuments() != null) {
            for (UserDocument doc : citizenDetails.getDocuments()) {
                if (doc != null && !combined.contains(doc)) {
                    combined.add(doc);
                }
            }
        }
        return combined;
    }
}
