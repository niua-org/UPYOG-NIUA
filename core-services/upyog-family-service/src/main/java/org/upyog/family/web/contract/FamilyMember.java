package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.upyog.family.domain.model.ExitReason;
import org.upyog.family.domain.model.MemberStatus;

/**
 * Contract representing an individual family member mapped to a citizen account in UPYOG.
 *
 * <p>The {@code relationshipWithHead} is represented as a String resolved dynamically against
 * UPYOG MDMS master data (module 'common-masters', master 'FamilyRelationship'), enabling
 * runtime additions without requiring service redeployment.</p>
 *
 * <p>Maps to database table {@code ug_family_member} maintaining active status, Aadhaar consent token,
 * historical exit audit data, and enriched citizen profile demographics.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMember {

    /**
     * Unique internal UUID primary key for the family member entry in {@code ug_family_member}.
     */
    @JsonProperty("id")
    private String id;

    /**
     * The business identifier of the parent family unit (e.g. {@code FAM-2026-0001}).
     */
    @JsonProperty("familyId")
    private String familyId;

    /**
     * Tenant identifier scoping the member record (e.g. {@code pg.citya}).
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * The unique citizen {@code uuid} corresponding to the citizen record in {@code egov-user}.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Transient display name of the member, populated from citizen demographics or staging.
     */
    @JsonProperty("name")
    private String name;

    /**
     * Transient mobile number of the member, used for lookup or display.
     */
    @JsonProperty("mobileNumber")
    private String mobileNumber;

    /**
     * Transient gender code (e.g. {@code MALE}, {@code FEMALE}, {@code TRANSGENDER}).
     */
    @JsonProperty("gender")
    private String gender;

    /**
     * Relationship with the Head of Family, dynamically validated against MDMS (e.g. {@code SELF}, {@code SPOUSE}, {@code SON}, {@code FATHER}).
     */
    @JsonProperty("relationshipWithHead")
    private String relationshipWithHead;

    /**
     * Citizen photo filestore ID or URL for UI card presentation (standardized to 'photo' across all services).
     */
    @JsonProperty("photo")
    private String photo;

    /**
     * Citizen date of birth as epoch milliseconds for UI presentation.
     */
    @JsonProperty("dob")
    private Long dob;

    /**
     * Flag indicating whether this citizen is currently the designated Head of Family for the unit.
     */
    @JsonProperty("isHead")
    private Boolean isHead;

    /**
     * Flag indicating whether this citizen's identity has been verified via Aadhaar e-KYC or OTP consent.
     */
    @JsonProperty("isVerified")
    private Boolean isVerified;

    /**
     * Active residency flag. Set to {@code false} when the member exits, splits, or migrates.
     */
    @JsonProperty("active")
    private Boolean active;

    /**
     * Current lifecycle state of the member (e.g. {@link MemberStatus#ACTIVE}, {@link MemberStatus#JOINED}, {@link MemberStatus#EXITED}).
     */
    @JsonProperty("status")
    private MemberStatus status;

    /**
     * Reason for member exit from the family unit (e.g. {@link ExitReason#MARRIAGE}, {@link ExitReason#DEATH}, {@link ExitReason#MIGRATION}).
     */
    @JsonProperty("exitReason")
    private ExitReason exitReason;

    /**
     * Epoch millisecond timestamp recording when the member exited the family unit.
     */
    @JsonProperty("exitDate")
    private Long exitDate;

    /**
     * Masked reference identifier or hash of the Aadhaar/HoF consent token validating the member's association.
     */
    @JsonProperty("consentToken")
    private String consentToken;

    /**
     * Epoch millisecond timestamp when consent was captured.
     */
    @JsonProperty("consentDate")
    private Long consentDate;

    /**
     * Epoch millisecond timestamp indicating the start of effective membership in this household unit.
     */
    @JsonProperty("effectiveFrom")
    private Long effectiveFrom;

    /**
     * Epoch millisecond timestamp marking when membership ceased (populated upon departure).
     */
    @JsonProperty("effectiveTo")
    private Long effectiveTo;

    /**
     * Dynamic extensible JSON metadata (e.g. occupation, marital status, education level).
     */
    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    /**
     * Synchronously enriched citizen demographic profile details fetched from {@code egov-user}.
     */
    @JsonProperty("citizenDetails")
    private CitizenProfile citizenDetails;

    /**
     * Standard UPYOG audit metadata recording actor UUIDs and epoch creation/modification timestamps.
     */
    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;

    /**
     * Resolves member relationshipWithHead, falling back to citizenDetails.
     *
     * @return relationship code
     */
    public String getRelationshipWithHead() {
        if (StringUtils.isNotBlank(relationshipWithHead)) {
            return relationshipWithHead;
        }
        return citizenDetails != null ? citizenDetails.getRelationshipWithHead() : null;
    }

    /**
     * Sets member relationshipWithHead.
     *
     * @param relationshipWithHead relationship code
     */
    public void setRelationshipWithHead(String relationshipWithHead) {
        this.relationshipWithHead = relationshipWithHead;
    }

    /**
     * Resolves member photo, falling back to citizenDetails.
     *
     * @return photo filestore ID or URL
     */
    public String getPhoto() {
        if (StringUtils.isNotBlank(photo)) {
            return photo;
        }
        return citizenDetails != null ? citizenDetails.getPhoto() : null;
    }

    /**
     * Sets member photo.
     *
     * @param photo photo filestore ID or URL
     */
    public void setPhoto(String photo) {
        this.photo = photo;
    }

    /**
     * Resolves member display name, falling back to citizenDetails.
     *
     * @return citizen name
     */
    public String getName() {
        if (StringUtils.isNotBlank(name)) {
            return name;
        }
        return citizenDetails != null ? citizenDetails.getName() : null;
    }

    /**
     * Resolves member mobile number, falling back to citizenDetails.
     *
     * @return citizen mobile number
     */
    public String getMobileNumber() {
        if (StringUtils.isNotBlank(mobileNumber)) {
            return mobileNumber;
        }
        return citizenDetails != null ? citizenDetails.getMobileNumber() : null;
    }

    /**
     * Resolves member gender, falling back to citizenDetails.
     *
     * @return citizen gender
     */
    public String getGender() {
        if (StringUtils.isNotBlank(gender)) {
            return gender;
        }
        return citizenDetails != null ? citizenDetails.getGender() : null;
    }

    /**
     * Resolves date of birth, falling back to citizenDetails.
     *
     * @return DOB in epoch milliseconds
     */
    public Long getDob() {
        if (dob != null) {
            return dob;
        }
        return citizenDetails != null ? citizenDetails.getDob() : null;
    }

    /**
     * Enriches transient member display attributes (name, photo, relation, mobile, gender, dob)
     * from the resolved {@link CitizenProfile} domain object.
     *
     * @param profile citizen profile retrieved from user service
     */
    public void enrichFromCitizenProfile(CitizenProfile profile) {
        if (profile == null) {
            return;
        }
        this.citizenDetails = profile;

        if (StringUtils.isNotBlank(profile.getName())) {
            this.name = profile.getName();
        }
        if (StringUtils.isNotBlank(profile.getMobileNumber())) {
            this.mobileNumber = profile.getMobileNumber();
        }
        if (StringUtils.isNotBlank(profile.getGender())) {
            this.gender = profile.getGender();
        }
        if (StringUtils.isNotBlank(profile.getPhoto())) {
            this.photo = profile.getPhoto();
        }
        if (profile.getDob() != null) {
            this.dob = profile.getDob();
        }
        if (Boolean.TRUE.equals(profile.getIsIdentityVerified())) {
            this.isVerified = true;
        }

        String effectiveRelationship = StringUtils.isNotBlank(this.relationshipWithHead)
                ? this.relationshipWithHead
                : profile.getRelationshipWithHead();
        if (StringUtils.isNotBlank(effectiveRelationship)) {
            this.relationshipWithHead = effectiveRelationship;
            profile.setRelationshipWithHead(effectiveRelationship);
        }
    }
}
