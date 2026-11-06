package org.egov.garbageservice.web.models.garbage;

import org.egov.garbageservice.web.models.workflow.WorkflowRequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.annotations.CustomSafeHtml;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Primary domain model for Garbage Service applications (persisted in ug_gc_application).
 * Merged dummy tables (ug_grbg_application, ug_grbg_old_details) and supports multi-tenant floors.
 */
@AllArgsConstructor
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(exclude = {"id", "uuid", "auditDetails", "workflow", "childGarbageApplications", "connectionLogs"})
public class GarbageApplication {

    @JsonProperty("id")
    private Long id;

    @CustomSafeHtml
    @JsonProperty("uuid")
    private String uuid;

    @CustomSafeHtml
    @JsonProperty("ownerUuid")
    private String ownerUuid;

    @CustomSafeHtml
    @JsonProperty("tenantUuid")
    private String tenantUuid;

    @CustomSafeHtml
    @JsonProperty("applicationNo")
    private String applicationNo;

    @CustomSafeHtml
    @JsonProperty("oldGarbageNo")
    private String oldGarbageNo;

    @CustomSafeHtml
    @JsonProperty("garbageNo")
    private String garbageNo;

    @CustomSafeHtml
    @JsonProperty("propertyId")
    private String propertyId;

    public String getPropertyId() {
        if (StringUtils.isNotBlank(propertyId)) {
            return propertyId;
        }
        if (propertyLocation != null && StringUtils.isNotBlank(propertyLocation.getPropertyId())) {
            this.propertyId = propertyLocation.getPropertyId();
            return propertyId;
        }
        return propertyId;
    }

    @CustomSafeHtml
    @JsonProperty("status")
    private String status;

    @CustomSafeHtml
    @JsonProperty("connectionStatus")
    @Builder.Default
    private String connectionStatus = "ACTIVE";

    @JsonProperty("arrears")
    @Builder.Default
    private BigDecimal arrears = BigDecimal.ZERO;

    @CustomSafeHtml
    @JsonProperty("tenantId")
    private String tenantId;

    @CustomSafeHtml
    @JsonProperty("businessService")
    @Builder.Default
    private String businessService = "garbage-service";

    @CustomSafeHtml
    @JsonProperty("channel")
    private String channel;

    @JsonProperty("dueDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dueDate;

    @CustomSafeHtml
    @JsonProperty("paymentStatus")
    private String paymentStatus;

    @JsonProperty("paymentAmount")
    @Builder.Default
    private BigDecimal paymentAmount = BigDecimal.ZERO;

    @JsonProperty("approvalDate")
    private Long approvalDate;

    @JsonProperty("additionalDetail")
    private JsonNode additionalDetail;

    @JsonProperty("garbageSpecification")
    private GarbageSpecification garbageSpecification;

    @JsonProperty("documents")
    @Builder.Default
    private List<GarbageDocument> documents = new ArrayList<>();

    @JsonProperty("connectionLogs")
    @Builder.Default
    private List<GarbageConnectionLog> connectionLogs = new ArrayList<>();

    @JsonProperty("applicantDetails")
    @Builder.Default
    private List<ApplicantDetail> applicantDetails = new ArrayList<>();

    @JsonProperty("propertyLocation")
    private PropertyLocation propertyLocation;

    @JsonProperty("workflow")
    private WorkflowRequest workflow;

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;

    // Hierarchy & cross-view support
    @JsonProperty("childGarbageApplications")
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonAlias({"childGarbageAccounts"})
    private List<GarbageApplication> childGarbageApplications = new ArrayList<>();

    @CustomSafeHtml
    @JsonProperty("parentApplicationNo")
    private String parentApplicationNo;

    // Backward compatibility helper fields / getters
    public Long getGarbageId() {
        return id;
    }

    public void setGarbageId(Long garbageId) {
        if (this.id == null) {
            this.id = garbageId;
        }
    }

    public String getGrbgApplicationNumber() {
        return applicationNo;
    }

    public void setGrbgApplicationNumber(String grbgApplicationNumber) {
        this.applicationNo = grbgApplicationNumber;
    }

    public String getOldGarbageId() {
        return oldGarbageNo;
    }

    public void setOldGarbageId(String oldGarbageId) {
        this.oldGarbageNo = oldGarbageId;
    }

    @JsonIgnore
    public Boolean getIsOwner() {
        if (garbageSpecification != null && StringUtils.isNotBlank(garbageSpecification.getPropertyType())) {
            return "OWNER".equalsIgnoreCase(garbageSpecification.getPropertyType());
        }
        return tenantUuid == null;
    }

    public void setIsOwner(Boolean isOwner) {
        if (garbageSpecification != null && isOwner != null) {
            garbageSpecification.setPropertyType(isOwner ? "OWNER" : "TENANT");
        }
    }

    public String getFloorNo() {
        return garbageSpecification != null ? garbageSpecification.getFloorNo() : null;
    }

    public void setFloorNo(String floorNo) {
        if (garbageSpecification == null) {
            garbageSpecification = new GarbageSpecification();
        }
        garbageSpecification.setFloorNo(floorNo);
    }

    public String getName() {
        if (!applicantDetails.isEmpty() && applicantDetails.get(0) != null) {
            String name = applicantDetails.get(0).getName();
            return StringUtils.isNotBlank(name) ? name : applicantDetails.get(0).getApplicantName();
        }
        return null;
    }

    public void setName(String name) {
        if (applicantDetails.isEmpty()) {
            applicantDetails.add(ApplicantDetail.builder().name(name).build());
        } else {
            applicantDetails.get(0).setName(name);
        }
    }

    public String getMobileNumber() {
        if (!applicantDetails.isEmpty() && applicantDetails.get(0) != null) {
            return applicantDetails.get(0).getMobileNumber();
        }
        return null;
    }

    public void setMobileNumber(String mobileNumber) {
        if (applicantDetails.isEmpty()) {
            applicantDetails.add(ApplicantDetail.builder().mobileNumber(mobileNumber).build());
        } else {
            applicantDetails.get(0).setMobileNumber(mobileNumber);
        }
    }

    public String getEmailId() {
        if (!applicantDetails.isEmpty() && applicantDetails.get(0) != null) {
            return applicantDetails.get(0).getEmailId();
        }
        return null;
    }

    public void setEmailId(String emailId) {
        if (applicantDetails.isEmpty()) {
            applicantDetails.add(ApplicantDetail.builder().emailId(emailId).build());
        } else {
            applicantDetails.get(0).setEmailId(emailId);
        }
    }

    @JsonProperty("grbgCollectionUnits")
    public List<GarbageSpecification> getGrbgCollectionUnits() {
        if (garbageSpecification != null) {
            return new ArrayList<>(Collections.singletonList(garbageSpecification));
        }
        return new ArrayList<>();
    }

    @JsonProperty("grbgCollectionUnits")
    public void setGrbgCollectionUnits(List<GarbageSpecification> grbgCollectionUnits) {
        if (grbgCollectionUnits != null && !grbgCollectionUnits.isEmpty()) {
            this.garbageSpecification = grbgCollectionUnits.get(0);
        }
    }

    @JsonIgnore
    public Boolean getIsActive() {
        return !"DISCONNECTED".equalsIgnoreCase(connectionStatus) && !"PERMANENTLY_DISCONNECTED".equalsIgnoreCase(connectionStatus);
    }
}
