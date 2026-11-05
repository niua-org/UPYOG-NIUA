package org.egov.garbageservice.web.models.garbage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.tracer.annotations.CustomSafeHtml;

/**
 * Domain model for collection parameters and specifications (persisted in ug_gc_specification).
 * Dropped obsolete flags (isVariableCalculation, isbulkgeneration, isAdditional).
 */
@AllArgsConstructor
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GarbageSpecification {

    @CustomSafeHtml
    @JsonProperty("id")
    private String id;

    @JsonProperty("garbageId")
    private Long garbageId;

    @CustomSafeHtml
    @JsonProperty("oldGarbageNo")
    private String oldGarbageNo;

    @CustomSafeHtml
    @JsonProperty("collectionType")
    private String collectionType;

    @CustomSafeHtml
    @JsonProperty("propertyType")
    private String propertyType; // OWNER or TENANT

    @CustomSafeHtml
    @JsonProperty("category")
    private String category;

    @CustomSafeHtml
    @JsonProperty("subCategory")
    private String subCategory;

    @CustomSafeHtml
    @JsonProperty("subCategoryType")
    private String subCategoryType;

    @JsonProperty("no_of_units")
    @Builder.Default
    private Integer no_of_units = 1;

    @CustomSafeHtml
    @JsonProperty("specialCategory")
    private String specialCategory;

    @CustomSafeHtml
    @JsonProperty("floorNo")
    private String floorNo;

    @JsonProperty("isInheritance")
    @Builder.Default
    private Boolean isInheritance = false;

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;

    // Backward compatibility aliases
    public String getOldGarbageId() {
        return oldGarbageNo;
    }

    public void setOldGarbageId(String oldGarbageId) {
        this.oldGarbageNo = oldGarbageId;
    }

    public String getTypeOfCollection() {
        return collectionType;
    }

    public void setTypeOfCollection(String typeOfCollection) {
        this.collectionType = typeOfCollection;
    }

    public String getUnitType() {
        return collectionType;
    }

    public void setUnitType(String unitType) {
        this.collectionType = unitType;
    }

    public String getPropertyOwnerType() {
        return propertyType;
    }

    public void setPropertyOwnerType(String propertyOwnerType) {
        this.propertyType = propertyOwnerType;
    }

    public Integer getNoOfUnits() {
        return no_of_units;
    }

    public void setNoOfUnits(Integer noOfUnits) {
        this.no_of_units = noOfUnits;
    }

    public Boolean getIsmonthlybilling() {
        return true;
    }
}
