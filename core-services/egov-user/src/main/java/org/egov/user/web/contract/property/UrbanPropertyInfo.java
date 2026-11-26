package org.egov.user.web.contract.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates urban property assessment details, owners, and linking eligibility.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrbanPropertyInfo {

    /**
     * Unique property assessment identifier assigned by Property Tax service.
     */
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * Tenant identifier for the municipality where the property is situated.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Current workflow / assessment status of the property (e.g. ACTIVE, INWORKFLOW).
     */
    @JsonProperty("status")
    private String status;

    /**
     * Property classification type (e.g. BUILTUP.SHAREDPROPERTY, VACANT).
     */
    @JsonProperty("propertyType")
    private String propertyType;

    /**
     * Ownership category (e.g. INDIVIDUAL.SINGLEOWNER, INDIVIDUAL.MULTIPLEOWNERS, JOINT).
     */
    @JsonProperty("ownershipCategory")
    private String ownershipCategory;

    /**
     * Formatted civic/door/locality address of the urban property.
     */
    @JsonProperty("urbanAddress")
    private String urbanAddress;

    /**
     * Total number of registered owners recorded in Property Tax records.
     */
    @JsonProperty("totalOwners")
    private Integer totalOwners;

    /**
     * Number of citizen profiles currently linked to this property in the database.
     */
    @JsonProperty("linkedOwnersCount")
    private Integer linkedOwnersCount;

    /**
     * Indicates whether this property can currently accept another citizen linkage.
     * True if single-owner and unlinked, or multi-owner with available unlinked slots.
     */
    @JsonProperty("eligibleForLinking")
    private Boolean eligibleForLinking;

    /**
     * List of registered property owners with masked contact details and linkage flags.
     */
    @JsonProperty("owners")
    @Builder.Default
    private List<UrbanPropertyOwnerInfo> owners = new ArrayList<>();
}
