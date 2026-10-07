package org.egov.nationaldashboardingest.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Existing Kafka / persister payload for one external API audit event.
 * Field names and JSON keys are unchanged; {@code state} is the only additive column.
 * Two events (INITIATED then SUCCESS/FAILED) share {@code correlationId} so persister
 * upserts a single {@code ug_external_api_message_detail} row.
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

    /**
     * Source UPYOG service ({@code external.api.audit.source-service}).
     */
    @JsonProperty("state")
    private String state;

    @JsonProperty("externalApiName")
    private String externalApiName;

    @JsonProperty("direction")
    private String direction;

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
