package org.egov.user.domain.model.asset;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
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
 * Domain entity representing a declared property asset attached to a citizen in UPYOG.
 *
 * <p>Persisted across Third Normal Form (3NF) tables:
 * {@code ug_user_property}, {@code ug_user_urban_property}, {@code ug_user_rural_property},
 * and {@code ug_user_property_owner}.</p>
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
public class UserProperty {

    /**
     * Unique internal UUID primary key for the property record in {@code ug_user_property}.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Citizen {@code uuid} owning this property record.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Tenant identifier scoping the property record.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Locality classification: {@code URBAN} or {@code RURAL}.
     */
    @JsonProperty("localityType")
    private String localityType;

    /**
     * Ownership category: {@code INDIVIDUAL}, {@code JOINT}, or {@code ORGANISATION}.
     */
    @JsonProperty("ownershipType")
    private String ownershipType;

    /**
     * Citizen declaration consent flag confirming authenticity of the declared asset.
     */
    @JsonProperty("declarationConfirmed")
    private Boolean declarationConfirmed;

    /**
     * Foreign key reference to attached verification document in {@code ug_user_document}.
     */
    @JsonProperty("documentId")
    private String documentId;

    /**
     * Attached verification document contract for convenience.
     */
    @Valid
    @JsonProperty("document")
    private UserDocument document;

    /**
     * Lifecycle status of the property record (defaults to {@code ACTIVE}).
     */
    @JsonProperty("status")
    private String status;

    /**
     * Extensible dynamic JSON metadata stored as JSONB.
     */
    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    /**
     * Specialization details for urban properties (1:1 with ug_user_property).
     */
    @Valid
    @JsonProperty("urbanPropertyDetail")
    private UrbanPropertyDetail urbanPropertyDetail;

    /**
     * Specialization details for rural properties (1:1 with ug_user_property).
     */
    @Valid
    @JsonProperty("ruralPropertyDetail")
    private RuralPropertyDetail ruralPropertyDetail;

    /**
     * List of co-owners affiliated with this property asset.
     */
    @Valid
    @JsonProperty("owners")
    @JsonAlias({"coOwners", "propertyOwners"})
    @Builder.Default
    private List<PropertyOwner> owners = new ArrayList<>();

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
