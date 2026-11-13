package org.egov.user.domain.model.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Domain entity representing rural property specialization details in {@code ug_user_rural_property}.
 *
 * <p>Linked 1:1 with {@link UserProperty} via {@code property_record_id}.</p>
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
public class RuralPropertyDetail {

    /**
     * Primary key and foreign key referencing {@link UserProperty#getId()}.
     */
    @JsonProperty("propertyRecordId")
    private String propertyRecordId;

    /**
     * Compatibility alias for {@code propertyRecordId}.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Tenant identifier scoping the rural property.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Gram Panchayat administrative jurisdiction.
     */
    @JsonProperty("gramPanchayat")
    private String gramPanchayat;

    /**
     * Revenue village name.
     */
    @JsonProperty("villageName")
    private String villageName;

    /**
     * Ward or block number within the Gram Panchayat.
     */
    @JsonProperty("wardNumber")
    private String wardNumber;

    /**
     * Physical rural address description.
     */
    @JsonProperty("propertyAddress")
    private String propertyAddress;

    /**
     * Property classification (e.g. RESIDENTIAL, AGRICULTURAL).
     */
    @JsonProperty("propertyType")
    private String propertyType;

    /**
     * Property usage classification (e.g. SELF_OCCUPIED, RENTED).
     */
    @JsonProperty("propertyUsage")
    private String propertyUsage;

    /**
     * Land revenue survey or Khasra plot identifier.
     */
    @JsonProperty("surveyNumber")
    private String surveyNumber;

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

    /**
     * Resolves effective property record ID between {@code propertyRecordId} and {@code id}.
     *
     * @return effective property record ID string
     */
    public String resolvePropertyRecordId() {
        return propertyRecordId != null ? propertyRecordId : id;
    }
}
