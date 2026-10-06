package org.egov.externalaudit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Kafka / persister payload for one audit event.
 * <p>
 * Two events (INITIATED then SUCCESS/FAILED) share {@link #correlationId} so persister
 * upserts a single {@code ug_external_api_message_detail} row.
 * </p>
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

    /**
     * UUID of the raw-payload row. Set on the INITIATED event; SUCCESS/FAILED may omit it
     * because persister upserts raw detail on {@code correlation_id}.
     */
    @JsonProperty("rawDetailId")
    private String rawDetailId;

    /**
     * Unique per logical request. Persister {@code ON CONFLICT} key.
     */
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
