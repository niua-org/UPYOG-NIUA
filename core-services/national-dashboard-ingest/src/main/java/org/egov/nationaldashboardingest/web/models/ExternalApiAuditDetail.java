package org.egov.nationaldashboardingest.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Existing production persistence model for external API audit.
 * Runtime publishing uses the library copy
 * {@link org.egov.externalaudit.model.ExternalApiAuditDetail} with the same JSON keys.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ExternalApiAuditDetail {

    @JsonProperty("id")
    private String id;

    @JsonProperty("rawDetailId")
    private String rawDetailId;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("externalService")
    private String externalService;

    @JsonProperty("externalApiName")
    private String externalApiName;

    @JsonProperty("direction")
    private String direction;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("method")
    private String method;

    @JsonProperty("originatingCorrelationId")
    private String originatingCorrelationId;

    @JsonProperty("businessReferenceId")
    private String businessReferenceId;

    @JsonProperty("requestTime")
    private Long requestTime;

    @JsonProperty("status")
    private String status;

    @JsonProperty("httpStatusCode")
    private Integer httpStatusCode;

    @JsonProperty("responseTime")
    private Long responseTime;

    @JsonProperty("durationMs")
    private Long durationMs;

    @JsonProperty("retryCount")
    private Integer retryCount;

    @JsonProperty("createdTime")
    private Long createdTime;

    @JsonProperty("lastModifiedTime")
    private Long lastModifiedTime;

    @JsonProperty("requestPayload")
    private Object requestPayload;

    @JsonProperty("responsePayload")
    private Object responsePayload;

    @JsonProperty("payloadSizeBytes")
    private Long payloadSizeBytes;

    @JsonProperty("errorDetails")
    private ExternalApiErrorDetails errorDetails;
}
