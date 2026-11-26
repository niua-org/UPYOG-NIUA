package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Enriched citizen demographic profile resolved from UPYOG user services.
 *
 * <p>Encapsulates personal demographics, identity validation status, social category,
 * parental details, and attached identification documents.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenProfile {

    /**
     * Unique citizen user UUID from {@code egov-user}.
     */
    @JsonProperty("uuid")
    private String uuid;

    /**
     * Citizen login username (standardized to mobile number in UPYOG).
     */
    @JsonProperty("userName")
    private String userName;

    /**
     * Full legal name of the citizen.
     */
    @JsonProperty("name")
    private String name;

    /**
     * Primary 10-digit mobile number of the citizen.
     */
    @JsonProperty("mobileNumber")
    private String mobileNumber;

    /**
     * Gender code: {@code MALE}, {@code FEMALE}, or {@code TRANSGENDER}.
     */
    @JsonProperty("gender")
    private String gender;

    /**
     * Date of birth represented as epoch milliseconds.
     */
    @JsonProperty("dob")
    private Long dob;

    /**
     * Profile photograph FileStore ID or URL.
     */
    @JsonProperty("photo")
    private String photo;

    /**
     * Dynamic relationship code with Head of Family.
     */
    @JsonProperty("relationshipWithHead")
    private String relationshipWithHead;

    /**
     * Primary email address of the citizen.
     */
    @JsonProperty("emailId")
    private String emailId;

    /**
     * Alternate or secondary contact number.
     */
    @JsonProperty("alternateMobileNumber")
    private String alternateMobileNumber;

    /**
     * Father's full legal name.
     */
    @JsonProperty("fatherName")
    private String fatherName;

    /**
     * Mother's full legal name.
     */
    @JsonProperty("motherName")
    private String motherName;

    /**
     * Spouse's full legal name if married.
     */
    @JsonProperty("spouseName")
    private String spouseName;

    /**
     * Guardian's full legal name if applicable.
     */
    @JsonProperty("guardian")
    private String guardian;

    /**
     * Relationship with guardian (e.g. {@code FATHER}, {@code MOTHER}, {@code HUSBAND}).
     */
    @JsonProperty("guardianRelation")
    private String guardianRelation;

    /**
     * Socio-economic caste classification code (e.g. {@code GENERAL}, {@code OBC}, {@code SC}, {@code ST}).
     */
    @JsonProperty("caste")
    private String caste;

    /**
     * Specially abled / divyang indicator flag.
     */
    @JsonProperty("isSpeciallyAbled")
    private Boolean isSpeciallyAbled;

    /**
     * Attached demographic and identity verification proofs (Aadhaar, Caste Certificate, DOB Proof).
     */
    @JsonProperty("documents")
    @Builder.Default
    private List<UserDocument> documents = new ArrayList<>();

    /**
     * Identity authentication flag confirming verification via Aadhaar e-KYC or official registry.
     */
    @JsonProperty("isIdentityVerified")
    private Boolean isIdentityVerified;
}
