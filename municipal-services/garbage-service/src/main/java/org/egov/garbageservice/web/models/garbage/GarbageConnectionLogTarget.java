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
 * Domain model representing an individual target connection within a multi-target lifecycle request
 * (persisted in ug_gc_connection_log_target).
 */
@AllArgsConstructor
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GarbageConnectionLogTarget {

    @CustomSafeHtml
    @JsonProperty("id")
    private String id;

    @CustomSafeHtml
    @JsonProperty("connectionLogId")
    private String connectionLogId;

    @JsonProperty("garbageId")
    private Long garbageId;

    @CustomSafeHtml
    @JsonProperty("applicationNo")
    private String applicationNo;

    @CustomSafeHtml
    @JsonProperty("prevConnectionStatus")
    private String prevConnectionStatus;

    @JsonProperty("createdTime")
    private Long createdTime;
}

