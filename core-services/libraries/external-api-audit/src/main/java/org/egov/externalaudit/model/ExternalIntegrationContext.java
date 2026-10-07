package org.egov.externalaudit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-call metadata supplied by the consuming service to {@link org.egov.externalaudit.service.ExternalApiAuditLogger}.
 * <p>
 * Leave {@link #correlationId} blank to allocate a new audit UUID (new logical request).
 * Reuse the same id and increment {@link #retryCount} only when retrying that same request
 * (for example HTTP 401 then token refresh). The next scheduler cycle must use a new id.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalIntegrationContext {

    /**
     * Unique audit row key persisted as {@code ug_external_api_message_detail.correlation_id}.
     * Generated when blank.
     */
    private String correlationId;

    /**
     * Tenant written to {@code tenant_id}. PFMS flows use {@code PFMS}; ingest uses the ULB code.
     */
    private String tenantId;

    /**
     * Source UPYOG service, persisted in column {@code state}.
     * Defaults to {@code external.api.audit.source-service} when blank.
     */
    private String state;

    /**
     * Stable integration name, for example {@code pfms-data-push}.
     */
    private String externalApiName;

    /**
     * {@code INBOUND} or {@code OUTBOUND}. Set by the logger; callers normally omit it.
     */
    private String direction;

    /**
     * Request body (or DTO). Masked and stored inside the payload envelope.
     */
    private Object requestPayload;

    /**
     * {@code 0} on first attempt. Increment on an in-cycle retry of the same {@link #correlationId}.
     */
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Tracer / RequestInfo / business correlation id stored inside the payload envelope,
     * not used as the table unique key.
     */
    private String originatingCorrelationId;

    /**
     * Business entity id (for example PFMS transaction id) stored inside the payload envelope.
     */
    private String businessReferenceId;

    /**
     * Target URL or inbound path. Stored in raw JSON as {@code endpoint}, not as a table column.
     */
    private String endpoint;

    /**
     * HTTP method of the integration call, for example {@code POST}.
     * Stored in raw JSON as both {@code httpMethod} and {@code method}.
     */
    private String httpMethod;
}
