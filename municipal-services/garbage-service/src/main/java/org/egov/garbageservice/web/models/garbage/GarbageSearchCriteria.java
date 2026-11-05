package org.egov.garbageservice.web.models.garbage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.annotations.CustomSafeHtml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Filter criteria for searching garbage applications.
 */
@AllArgsConstructor
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GarbageSearchCriteria {

    @CustomSafeHtml
    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("applicationNumber")
    private List<String> applicationNumber;

    @CustomSafeHtml
    @JsonProperty("applicationNo")
    private String applicationNo;

    @JsonProperty("garbageNumber")
    private List<String> garbageNumber;

    @CustomSafeHtml
    @JsonProperty("garbageNo")
    private String garbageNo;

    @JsonProperty("propertyId")
    private List<String> propertyId;

    @CustomSafeHtml
    @JsonProperty("propertyIdString")
    private String propertyIdString;

    @CustomSafeHtml
    @JsonProperty("ownerUuid")
    private String ownerUuid;

    @CustomSafeHtml
    @JsonProperty("tenantUuid")
    private String tenantUuid;

    @CustomSafeHtml
    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @JsonProperty("status")
    private List<String> status;

    @CustomSafeHtml
    @JsonProperty("connectionStatus")
    private String connectionStatus;

    @CustomSafeHtml
    @JsonProperty("channel")
    private String channel;

    @CustomSafeHtml
    @JsonProperty("businessService")
    private String businessService;

    @JsonProperty("offset")
    private Long offset;

    @JsonProperty("limit")
    private Long limit;

    @JsonProperty("fromDate")
    private Long fromDate;

    @JsonProperty("toDate")
    private Long toDate;

    @JsonProperty("ids")
    private List<Long> ids;

    @JsonProperty("uuids")
    private List<String> uuids;

    @JsonProperty("isOwner")
    private Boolean isOwner;

    @CustomSafeHtml
    @JsonProperty("requestType")
    private String requestType;

    @Builder.Default
    @JsonProperty("isCountCall")
    private Boolean isCountCall = false;

    @Builder.Default
    @JsonProperty("isSchedulerCall")
    private Boolean isSchedulerCall = false;

    @CustomSafeHtml
    @JsonProperty("orderBy")
    @Builder.Default
    private String orderBy = "DESC";

    @JsonProperty("createdBy")
    private List<String> createdBy;

    // Helper normalization methods
    public List<String> getResolvedApplicationNumbers() {
        if (applicationNumber != null && !applicationNumber.isEmpty()) {
            return applicationNumber;
        }
        if (StringUtils.isNotBlank(applicationNo)) {
            return Collections.singletonList(applicationNo);
        }
        return Collections.emptyList();
    }

    public List<String> getResolvedPropertyIds() {
        if (propertyId != null && !propertyId.isEmpty()) {
            return propertyId;
        }
        if (StringUtils.isNotBlank(propertyIdString)) {
            return Collections.singletonList(propertyIdString);
        }
        return Collections.emptyList();
    }

    public List<String> getResolvedGarbageNumbers() {
        if (garbageNumber != null && !garbageNumber.isEmpty()) {
            return garbageNumber;
        }
        if (StringUtils.isNotBlank(garbageNo)) {
            return Collections.singletonList(garbageNo);
        }
        return Collections.emptyList();
    }
}
