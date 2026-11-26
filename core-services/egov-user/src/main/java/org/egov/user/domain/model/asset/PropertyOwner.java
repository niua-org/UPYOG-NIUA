package org.egov.user.domain.model.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Domain entity representing property co-ownership specification in {@code ug_user_property_owner}.
 *
 * <p>Adheres strictly to Third Normal Form (3NF): citizen owners are linked exclusively via
 * {@code userUuid} (preventing demographic duplication), while organizational entities store
 * relevant details in {@code additionalDetails}.</p>
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
public class PropertyOwner {

    /**
     * Unique internal UUID primary key for the property owner record.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Foreign key reference to {@link UserProperty#getId()}.
     */
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * Tenant identifier scoping this ownership record.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Ownership category: {@code INDIVIDUAL}, {@code JOINT_OWNER}, or {@code ORGANISATION}.
     */
    @JsonProperty("ownerType")
    private String ownerType;

    /**
     * Citizen {@code uuid} in {@code eg_user}. Null for non-individual/organizational owners.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Active ownership state flag.
     */
    @JsonProperty("active")
    @Builder.Default
    private Boolean active = true;

    /**
     * Extensible dynamic JSON metadata (e.g. organisation details) stored as JSONB.
     */
    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    /**
     * Actor UUID who created this record.
     */
    @JsonProperty("createdBy")
    private String createdBy;

    /**
     * Epoch millisecond timestamp of record creation.
     */
    @JsonProperty("createdTime")
    private Long createdTime;

    /**
     * Actor UUID who last modified this record.
     */
    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy;

    /**
     * Epoch millisecond timestamp of last modification.
     */
    @JsonProperty("lastModifiedTime")
    private Long lastModifiedTime;
}
