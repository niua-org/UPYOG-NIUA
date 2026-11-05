package org.egov.garbageservice.web.models.garbage;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.tracer.annotations.CustomSafeHtml;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain model for disconnection, reconnection, and permanent disconnection lifecycle requests
 * (persisted in ug_gc_connection_log).
 */
@AllArgsConstructor
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GarbageConnectionLog {

    @CustomSafeHtml
    @JsonProperty("id")
    private String id;

    @JsonProperty("garbageId")
    private Long garbageId;

    @CustomSafeHtml
    @JsonProperty("applicationNo")
    private String applicationNo;

    @CustomSafeHtml
    @JsonProperty("requestNo")
    private String requestNo;

    @CustomSafeHtml
    @JsonProperty("requestType")
    private String requestType; // DISCONNECTION, RECONNECTION, PERMANENT_DISCONNECTION

    @CustomSafeHtml
    @JsonProperty("scope")
    @Builder.Default
    private String scope = "SELECTED"; // PROPERTY, SELECTED

    @CustomSafeHtml
    @JsonProperty("propertyId")
    private String propertyId;

    @CustomSafeHtml
    @JsonProperty("workflowStatus")
    private String workflowStatus;

    @CustomSafeHtml
    @JsonProperty("requestedByUuid")
    private String requestedByUuid;

    @CustomSafeHtml
    @JsonProperty("approvedByUuid")
    private String approvedByUuid;

    @JsonProperty("approvedTime")
    private Long approvedTime;

    @CustomSafeHtml
    @JsonProperty("rejectionReason")
    private String rejectionReason;

    @JsonProperty("requestSnapshot")
    private JsonNode requestSnapshot;

    @CustomSafeHtml
    @JsonProperty("status")
    private String status; // INITIATED, APPROVED, REJECTED

    @CustomSafeHtml
    @JsonProperty("reason")
    private String reason;

    @CustomSafeHtml
    @JsonProperty("reasonDetails")
    private String reasonDetails;

    @JsonProperty("effectiveFrom")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate effectiveFrom;

    @JsonProperty("effectiveTo")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate effectiveTo;

    @JsonProperty("feeCharged")
    @Builder.Default
    private BigDecimal feeCharged = BigDecimal.ZERO;

    @JsonProperty("targets")
    @Builder.Default
    private List<GarbageConnectionLogTarget> targets = new ArrayList<>();

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
