package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Detailed attributes for urban properties linked via UPYOG Property Tax system.
 *
 * <p>Enables cross-module linkage with existing municipal tax assessments through the standard
 * {@code upyogPropertyId}.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrbanPropertyDetail {

    /**
     * Unique internal UUID primary key for the urban property detail record.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Foreign key reference linking to the parent {@link UserProperty#propertyId}.
     */
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * Unique property assessment identifier from UPYOG Property Tax system (e.g. {@code PG-PT-1013-008987}).
     */
    @JsonProperty("upyogPropertyId")
    private String upyogPropertyId;

    /**
     * Annual or assessed municipal property tax amount.
     */
    @JsonProperty("propertyTaxAmount")
    private BigDecimal propertyTaxAmount;

    /**
     * Complete physical municipal address of the urban property.
     */
    @JsonProperty("urbanAddress")
    private String urbanAddress;
}
