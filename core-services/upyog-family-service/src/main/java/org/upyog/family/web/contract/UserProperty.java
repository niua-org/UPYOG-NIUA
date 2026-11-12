package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite model representing a property asset declared by a family member in UPYOG.
 *
 * <p>Supports both Urban (UPYOG Property Tax integrated) and Rural (Gram Panchayat / Khasra survey)
 * property assets, along with co-ownership structures and verification proofs.</p>
 *
 * <p>Adheres to Single Responsibility Principle (SRP) by isolating property asset attributes
 * from familial lineage concerns.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProperty {

    /**
     * Unique internal UUID primary key for the user property record.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Business identifier of the property (e.g. {@code PROP-RURAL-2026-0091}).
     */
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * Citizen {@code uuid} of the declaring family member.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Tenant identifier inherited from the parent household unit.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Locality classification: {@code URBAN} or {@code RURAL}.
     */
    @JsonProperty("localityType")
    private String localityType;

    /**
     * Ownership mode: {@code INDIVIDUAL} or {@code JOINT}.
     */
    @JsonProperty("ownershipType")
    private String ownershipType;

    /**
     * Citizen declaration consent flag confirming authenticity of the declared asset.
     */
    @JsonProperty("declarationConfirmed")
    private Boolean declarationConfirmed;

    /**
     * Detailed landholding and panchayat metadata for rural properties.
     */
    @Valid
    @JsonProperty("ruralPropertyDetail")
    private RuralPropertyDetail ruralPropertyDetail;

    /**
     * Detailed municipal and property tax assessment attributes for urban properties.
     */
    @Valid
    @JsonProperty("urbanPropertyDetail")
    private UrbanPropertyDetail urbanPropertyDetail;

    /**
     * Roster of individual or organizational co-owners affiliated with this property asset.
     */
    @Valid
    @JsonProperty("owners")
    @Builder.Default
    private List<PropertyOwner> owners = new ArrayList<>();

    /**
     * Verification document (NOC, tax receipt, registry deed) certifying property ownership.
     */
    @Valid
    @JsonProperty("document")
    private UserDocument document;

    /**
     * Standard UPYOG audit metadata tracking creation and modification actor UUIDs and timestamps.
     */
    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
