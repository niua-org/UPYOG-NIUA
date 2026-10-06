package org.egov.externalaudit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalIntegrationContext {

    /**
     * Unique audit row key. Generated when blank. Must be unique per logical request.
     * Reuse the same value when retrying the same request so the existing row is updated.
     */
    private String correlationId;

    private String tenantId;

    /**
     * Source service identifier persisted in {@code ug_external_api_message_detail.state}.
     */
    private String state;

    private String externalApiName;

    private String direction;

    private Object requestPayload;

    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Tracer / RequestInfo / business correlation id stored inside the payload envelope.
     */
    private String originatingCorrelationId;

    /**
     * Business entity id (e.g. PFMS transaction id) stored inside the payload envelope.
     */
    private String businessReferenceId;

    private String endpoint;

    private String httpMethod;
}
