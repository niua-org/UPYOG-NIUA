package org.egov.user.web.contract.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.user.domain.model.UserDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Enriched citizen demographic profile details contract transferred between services.
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
public class CitizenProfileDetail {

    /**
     * Unique citizen user UUID in {@code eg_user}.
     */
    @JsonProperty("uuid")
    private String uuid;

    /**
     * Citizen login username.
     */
    @JsonProperty("userName")
    private String userName;

    /**
     * Full legal name of the citizen.
     */
    @JsonProperty("name")
    private String name;

    /**
     * Primary 10-digit mobile number.
     */
    @JsonProperty("mobileNumber")
    private String mobileNumber;

    /**
     * Gender string (MALE, FEMALE, TRANSGENDER).
     */
    @JsonProperty("gender")
    private String gender;

    /**
     * Date of birth represented as epoch milliseconds or timestamp.
     */
    @JsonProperty("dob")
    private Long dob;

    /**
     * Profile photograph FileStore ID or URL.
     */
    @JsonProperty("photo")
    private String photo;

    /**
     * Primary email address.
     */
    @JsonProperty("emailId")
    private String emailId;

    /**
     * Alternate contact mobile number.
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
     * Relationship with guardian (e.g. FATHER, MOTHER, HUSBAND).
     */
    @JsonProperty("guardianRelation")
    private String guardianRelation;

    /**
     * Socio-economic caste classification code.
     */
    @JsonProperty("caste")
    private String caste;

    /**
     * Specially abled / divyang indicator flag.
     */
    @JsonProperty("isSpeciallyAbled")
    private Boolean isSpeciallyAbled;

    /**
     * Attached demographic and identity verification documents.
     */
    @JsonProperty("documents")
    @Builder.Default
    private List<UserDocument> documents = new ArrayList<>();

    /**
     * Identity authentication flag.
     */
    @JsonProperty("isIdentityVerified")
    private Boolean isIdentityVerified;
}
