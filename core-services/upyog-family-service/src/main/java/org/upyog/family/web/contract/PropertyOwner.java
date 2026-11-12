package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Co-ownership specification for declared citizen properties in UPYOG.
 *
 * <p>Adheres strictly to Third Normal Form (3NF): citizen owners are linked via {@code userUuid}
 * (preventing demographic redundancy), while organizational entities store relevant metadata
 * within {@code additionalDetails}.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyOwner {

    /**
     * Unique internal UUID primary key for the property owner record.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Business identifier of the associated property.
     */
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * Citizen {@code uuid} corresponding to the owner record in {@code egov-user}.
     * Null for non-individual/organizational owners.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Ownership category: {@code INDIVIDUAL}, {@code JOINT_OWNER}, or {@code ORGANISATION}.
     */
    @JsonProperty("ownerType")
    private String ownerType;

    /**
     * Active ownership state flag.
     */
    @JsonProperty("active")
    private Boolean active;

    /**
     * Extensible JSON metadata capturing organization registration or non-individual ownership attributes.
     */
    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    /**
     * Standard UPYOG audit metadata tracking creation and modification actor UUIDs and timestamps.
     */
    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
