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
 * Domain model for monthly garbage rent & service charge payments (persisted in ug_gc_monthly_rent_payment).
 */
@AllArgsConstructor
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GarbageMonthlyRentPayment {

    @CustomSafeHtml
    @JsonProperty("id")
    private String id;

    @JsonProperty("garbageId")
    private Long garbageId;

    @CustomSafeHtml
    @JsonProperty("applicationNo")
    private String applicationNo;

    @CustomSafeHtml
    @JsonProperty("billNo")
    private String billNo;

    @CustomSafeHtml
    @JsonProperty("demandId")
    private String demandId;

    @JsonProperty("billDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate billDate;

    @JsonProperty("periodFrom")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate periodFrom;

    @JsonProperty("periodTo")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate periodTo;

    @JsonProperty("billAmount")
    private BigDecimal billAmount;

    @CustomSafeHtml
    @JsonProperty("paymentStatus")
    @Builder.Default
    private String paymentStatus = "PENDING";

    @CustomSafeHtml
    @JsonProperty("tenant")
    private String tenant;

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
