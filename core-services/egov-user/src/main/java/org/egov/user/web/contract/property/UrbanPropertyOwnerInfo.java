package org.egov.user.web.contract.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encapsulates ownership info for an urban property owner.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrbanPropertyOwnerInfo {

    /**
     * Unique identifier / UUID of the property owner in Property Tax records.
     */
    @JsonProperty("uuid")
    private String uuid;

    /**
     * Full name of the property owner.
     */
    @JsonProperty("name")
    private String name;

    /**
     * Internal raw mobile number used strictly for OTP dispatch (never exposed in JSON).
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String rawMobileNumber;

    /**
     * Masked mobile number displayed to citizens for verification privacy (e.g. 98*****321).
     */
    @JsonProperty("maskedMobileNumber")
    private String maskedMobileNumber;

    /**
     * Flag indicating whether this owner is designated as the primary owner.
     */
    @JsonProperty("isPrimaryOwner")
    private Boolean isPrimaryOwner;

    /**
     * Ownership percentage held in the property asset.
     */
    @JsonProperty("ownerShipPercentage")
    private Double ownerShipPercentage;

    /**
     * Ownership category / type (e.g. NONE, INSTITUTIONAL).
     */
    @JsonProperty("ownerType")
    private String ownerType;

    /**
     * Indicates whether this specific owner has already linked their citizen profile to this property.
     */
    @JsonProperty("alreadyLinked")
    private Boolean alreadyLinked;
}
