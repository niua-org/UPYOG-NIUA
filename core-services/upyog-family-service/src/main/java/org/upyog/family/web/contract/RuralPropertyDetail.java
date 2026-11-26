package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Detailed attributes for rural landholdings and residential properties in UPYOG.
 *
 * <p>Captures administrative panchayat boundaries, revenue survey identifiers (Khasra),
 * and rural property utilization types.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuralPropertyDetail {

    /**
     * Unique internal UUID primary key for the rural property detail record.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Foreign key reference linking to the parent {@link UserProperty#propertyId}.
     */
    @JsonProperty("propertyId")
    private String propertyId;

    /**
     * Local Gram Panchayat governing authority (e.g. {@code Rampur GP}).
     */
    @JsonProperty("gramPanchayat")
    private String gramPanchayat;

    /**
     * Revenue village name (e.g. {@code Rampur Khas}).
     */
    @JsonProperty("villageName")
    private String villageName;

    /**
     * Ward or block designation within the panchayat.
     */
    @JsonProperty("wardNumber")
    private String wardNumber;

    /**
     * Complete physical location and street address of the rural property.
     */
    @JsonProperty("propertyAddress")
    private String propertyAddress;

    /**
     * Property classification code validated against MDMS (e.g. {@code RESIDENTIAL}, {@code AGRICULTURAL}).
     */
    @JsonProperty("propertyType")
    private String propertyType;

    /**
     * Usage classification code validated against MDMS (e.g. {@code SELF_OCCUPIED}, {@code RENTED}).
     */
    @JsonProperty("propertyUsage")
    private String propertyUsage;

    /**
     * Land revenue survey or Khasra plot identifier (e.g. {@code KHASRA-842/1}).
     */
    @JsonProperty("surveyNumber")
    private String surveyNumber;
}
