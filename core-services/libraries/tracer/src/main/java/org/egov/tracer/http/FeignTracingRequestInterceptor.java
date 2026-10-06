package org.egov.tracer.http;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.egov.tracer.config.TracerProperties;
import org.slf4j.MDC;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static org.egov.tracer.constants.TracerConstants.CORRELATION_ID_HEADER;
import static org.egov.tracer.constants.TracerConstants.CORRELATION_ID_MDC;
import static org.egov.tracer.constants.TracerConstants.TENANTID_MDC;
import static org.egov.tracer.constants.TracerConstants.TENANT_ID_HEADER;

/**
 * Feign equivalent of {@link RestTemplateLoggingInterceptor}.
 * <p>
 * Adds {@code x-correlation-id} and {@code tenantId} headers from MDC, and logs
 * the outbound request. Response logging is handled by {@link TracerFeignLogger}
 * so both HTTP clients share the same tracing behaviour.
 * </p>
 */
@Slf4j
public class FeignTracingRequestInterceptor implements RequestInterceptor {

    private static final String REQUEST_MESSAGE_WITH_BODY = "Sending request to {} with verb {} with body {}";
    private static final String REQUEST_MESSAGE = "Sending request to {} with verb {}";
    private static final String EMPTY_BODY = "<NOT-AVAILABLE>";

    private final TracerProperties tracerProperties;

    public FeignTracingRequestInterceptor(TracerProperties tracerProperties) {
        this.tracerProperties = tracerProperties;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (template.headers().get(CORRELATION_ID_HEADER) == null) {
            String correlationId = MDC.get(CORRELATION_ID_MDC);
            if (correlationId != null) {
                template.header(CORRELATION_ID_HEADER, correlationId);
            }
        }

        Collection<String> tenantHeaders = template.headers().get(TENANT_ID_HEADER);
        if (CollectionUtils.isEmpty(tenantHeaders)) {
            String tenantId = MDC.get(TENANTID_MDC);
            if (tenantId != null) {
                template.header(TENANT_ID_HEADER, tenantId);
            }
        }

        logRequest(template);
    }

    private void logRequest(RequestTemplate template) {
        String url = resolveUrl(template);
        String method = template.method();
        if (tracerProperties.isRestTemplateDetailedLoggingEnabled()) {
            log.info(REQUEST_MESSAGE_WITH_BODY, url, method, getBody(template));
        } else {
            log.info(REQUEST_MESSAGE, url, method);
        }
    }

    private String resolveUrl(RequestTemplate template) {
        if (template.feignTarget() != null && template.feignTarget().url() != null) {
            return template.feignTarget().url() + template.url();
        }
        return template.url();
    }

    private String getBody(RequestTemplate template) {
        if (template.body() == null) {
            return EMPTY_BODY;
        }
        return new String(template.body(), StandardCharsets.UTF_8);
    }
}
