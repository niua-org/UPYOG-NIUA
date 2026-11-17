package org.egov.user.web.contract.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.asset.PropertyOwner;
import org.egov.user.domain.model.asset.RuralPropertyDetail;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.domain.service.utils.UserConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Request payload envelope for standalone rural property CRUD operations (create, update, delete/deactivate).
 *
 * <p>Supports passing either a composite {@link UserProperty} or separate rural property fields
 * for seamless integration with member editing forms and administrative dashboards.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Request envelope for standalone rural property CRUD and lifecycle mutations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RuralPropertyRequest {

    /**
     * Standard UPYOG RequestInfo contract.
     */
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * Complete user property entity representing the declared rural property asset.
     */
    @Valid
    @JsonProperty("property")
    private UserProperty property;

    /**
     * Direct rural property specialization detail for lightweight forms.
     */
    @Valid
    @JsonProperty("ruralProperty")
    private RuralPropertyDetail ruralProperty;

    /**
     * Citizen {@code uuid} owning this property.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Alternate citizen UUID alias.
     */
    @JsonProperty("uuid")
    private String uuid;

    /**
     * Property primary key ID (required for update and delete/deactivation operations).
     */
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * Tenant identifier scoping the property.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Ownership category: {@code INDIVIDUAL}, {@code JOINT}, or {@code ORGANISATION}.
     */
    @JsonProperty("ownershipType")
    private String ownershipType;

    /**
     * Citizen declaration confirmation flag.
     */
    @JsonProperty("declarationConfirmed")
    private Boolean declarationConfirmed;

    /**
     * Lifecycle status override (e.g. {@code ACTIVE} or {@code INACTIVE}).
     */
    @JsonProperty("status")
    private String status;

    /**
     * List of joint owners or organisation owner details.
     */
    @Valid
    @JsonProperty("owners")
    @Builder.Default
    private List<PropertyOwner> owners = new ArrayList<>();

    /**
     * Attached proof document (e.g. land title, Jamabandi, Khasra extract).
     */
    @Valid
    @JsonProperty("document")
    private UserDocument document;

    /**
     * Extensible dynamic JSON metadata.
     */
    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    /**
     * Resolves citizen UUID from {@code userUuid}, {@code uuid}, or the embedded {@code property}.
     *
     * @return effective citizen user UUID string
     */
    public String resolveUserUuid() {
        if (StringUtils.isNotBlank(this.userUuid)) {
            return this.userUuid.trim();
        }
        if (StringUtils.isNotBlank(this.uuid)) {
            return this.uuid.trim();
        }
        if (this.property != null && StringUtils.isNotBlank(this.property.getUserUuid())) {
            return this.property.getUserUuid().trim();
        }
        return null;
    }

    /**
     * Resolves property primary key ID from {@code propertyId} or the embedded {@code property}.
     *
     * @return effective property ID string
     */
    public String resolvePropertyId() {
        if (StringUtils.isNotBlank(this.propertyId)) {
            return this.propertyId.trim();
        }
        if (this.property != null && StringUtils.isNotBlank(this.property.getId())) {
            return this.property.getId().trim();
        }
        if (this.ruralProperty != null && StringUtils.isNotBlank(this.ruralProperty.resolvePropertyRecordId())) {
            return this.ruralProperty.resolvePropertyRecordId().trim();
        }
        return null;
    }

    /**
     * Resolves tenant identifier from {@code tenantId}, embedded {@code property}, or {@code ruralProperty}.
     *
     * @return effective tenant ID string
     */
    public String resolveTenantId() {
        if (StringUtils.isNotBlank(this.tenantId)) {
            return this.tenantId.trim();
        }
        if (this.property != null && StringUtils.isNotBlank(this.property.getTenantId())) {
            return this.property.getTenantId().trim();
        }
        if (this.ruralProperty != null && StringUtils.isNotBlank(this.ruralProperty.getTenantId())) {
            return this.ruralProperty.getTenantId().trim();
        }
        return null;
    }

    /**
     * Assembles and resolves a unified {@link UserProperty} domain model from the request attributes.
     *
     * @return populated {@link UserProperty} entity ready for validation and persistence
     */
    public UserProperty resolveProperty() {
        UserProperty target = this.property != null ? this.property : new UserProperty();

        String effectiveUserUuid = resolveUserUuid();
        if (StringUtils.isNotBlank(effectiveUserUuid)) {
            target.setUserUuid(effectiveUserUuid);
        }

        String effectivePropertyId = resolvePropertyId();
        if (StringUtils.isNotBlank(effectivePropertyId)) {
            target.setId(effectivePropertyId);
        }

        String effectiveTenantId = resolveTenantId();
        if (StringUtils.isNotBlank(effectiveTenantId)) {
            target.setTenantId(effectiveTenantId);
        }

        target.setLocalityType(UserConstants.LOCALITY_TYPE_RURAL);

        if (StringUtils.isNotBlank(this.ownershipType)) {
            target.setOwnershipType(this.ownershipType);
        } else if (StringUtils.isBlank(target.getOwnershipType())) {
            target.setOwnershipType("INDIVIDUAL");
        }

        if (this.declarationConfirmed != null) {
            target.setDeclarationConfirmed(this.declarationConfirmed);
        }

        if (StringUtils.isNotBlank(this.status)) {
            target.setStatus(this.status);
        }

        if (this.additionalDetails != null) {
            target.setAdditionalDetails(this.additionalDetails);
        }

        if (this.document != null) {
            target.setDocument(this.document);
        }

        if (this.owners != null && !this.owners.isEmpty()) {
            target.setOwners(this.owners);
        }

        RuralPropertyDetail effectiveRural = this.ruralProperty != null ? this.ruralProperty : target.getRuralPropertyDetail();
        if (effectiveRural != null) {
            if (StringUtils.isBlank(effectiveRural.getTenantId())) {
                effectiveRural.setTenantId(target.getTenantId());
            }
            if (StringUtils.isBlank(effectiveRural.getPropertyRecordId())) {
                effectiveRural.setPropertyRecordId(target.getId());
            }
            target.setRuralPropertyDetail(effectiveRural);
        }

        return target;
    }
}
