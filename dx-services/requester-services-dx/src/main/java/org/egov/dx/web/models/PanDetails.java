package org.egov.dx.web.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Verified PAN entity details returned to UPYOG callers.
 * Excludes raw PAN, registered full name, and Aadhaar seeding status to protect citizen PII.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PanDetails {

    /**
     * Unmasked PAN (excluded in client responses for PII protection).
     */
    @JsonProperty("pan")
    private String pan;

    /**
     * Safely masked PAN for telemetry and UI display (e.g., "ABCDE****F").
     */
    @JsonProperty("maskedPan")
    private String maskedPan;

    /**
     * Registered full name (excluded in public verify responses to prevent PAN-to-name harvesting).
     */
    @JsonProperty("fullName")
    private String fullName;

    /**
     * Entity category resolved from PAN (e.g. "Individual", "Company").
     */
    @JsonProperty("category")
    private String category;

    /**
     * Official PAN status from ITD (e.g. "Existing and Valid", "Inoperative").
     */
    @JsonProperty("panStatus")
    private String panStatus;

    /**
     * Aadhaar seeding status flag (excluded in client responses for privacy).
     */
    @JsonProperty("aadhaarSeedingStatus")
    private String aadhaarSeedingStatus;

    /**
     * Boolean indicator whether applicant full name matched ITD records.
     */
    @JsonProperty("nameMatch")
    private Boolean nameMatch;

    /**
     * Boolean indicator whether applicant date of birth matched ITD records.
     */
    @JsonProperty("dobMatch")
    private Boolean dobMatch;
}
