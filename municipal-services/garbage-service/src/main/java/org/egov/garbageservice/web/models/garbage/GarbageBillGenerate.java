package org.egov.garbageservice.web.models.garbage;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.tracer.annotations.CustomSafeHtml;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain model for billing scheduler batch run execution log (persisted in ug_gc_bill_generate).
 */
@AllArgsConstructor
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GarbageBillGenerate {

    @CustomSafeHtml
    @JsonProperty("id")
    private String id;

    @CustomSafeHtml
    @JsonProperty("garbageId")
    private String garbageId;

    @CustomSafeHtml
    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("billingDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate billingDate;

    @JsonProperty("billingPeriodFrom")
    private Long billingPeriodFrom;

    @JsonProperty("billingPeriodTo")
    private Long billingPeriodTo;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("penaltyAmount")
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @CustomSafeHtml
    @JsonProperty("paymentType")
    private String paymentType;

    @CustomSafeHtml
    @JsonProperty("status")
    @Builder.Default
    private String status = "PENDING";

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
