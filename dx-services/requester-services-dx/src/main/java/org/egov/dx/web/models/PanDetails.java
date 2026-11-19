package org.egov.dx.web.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Verified PAN entity details returned to UPYOG callers.
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
public class PanDetails {

    @JsonProperty("pan")
    private String pan;

    @JsonProperty("maskedPan")
    private String maskedPan;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("category")
    private String category;

    @JsonProperty("panStatus")
    private String panStatus;

    @JsonProperty("aadhaarSeedingStatus")
    private String aadhaarSeedingStatus;

    @JsonProperty("nameMatch")
    private Boolean nameMatch;

    @JsonProperty("dobMatch")
    private Boolean dobMatch;
}
