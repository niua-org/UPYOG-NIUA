package org.egov.user.domain.model.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Domain entity representing urban property specialization details in {@code ug_user_urban_property}.
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
public class UrbanPropertyDetail {

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
     * Tenant identifier scoping the urban property.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Unique property identifier in UPYOG Property Tax system.
     */
    @JsonProperty("upyogPropertyId")
    private String upyogPropertyId;

    /**
     * Assessed property tax amount.
     */
    @JsonProperty("propertyTaxAmount")
    private BigDecimal propertyTaxAmount;

    /**
     * Full physical municipal address of the urban property.
     */
    @JsonProperty("urbanAddress")
    private String urbanAddress;

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
