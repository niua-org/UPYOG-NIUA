package org.egov.tracer.http;

import feign.Logger;
import feign.Request;
import feign.Response;
import feign.Util;
import lombok.extern.slf4j.Slf4j;
import org.egov.tracer.config.TracerProperties;

import java.io.IOException;

import static org.egov.tracer.constants.TracerConstants.EMPTY_BODY;
import static org.egov.tracer.constants.TracerConstants.FAILED_RESPONSE_MESSAGE;
import static org.egov.tracer.constants.TracerConstants.RESPONSE_MESSAGE;
import static org.egov.tracer.constants.TracerConstants.RESPONSE_MESSAGE_WITH_BODY;

/**
 * Feign logger that mirrors {@link RestTemplateLoggingInterceptor} response logging.
 * Request logging is done by {@link FeignTracingRequestInterceptor} to avoid duplicates.
 */
@Slf4j
public class TracerFeignLogger extends Logger {

    private final TracerProperties tracerProperties;

    public TracerFeignLogger(TracerProperties tracerProperties) {
        this.tracerProperties = tracerProperties;
    }

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
        // Request logging is owned by FeignTracingRequestInterceptor, matching RestTemplate.
    }

    @Override
    protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime)
            throws IOException {
        String url = response.request() != null ? response.request().url() : configKey;
        if (tracerProperties.isRestTemplateDetailedLoggingEnabled() && response.body() != null) {
            byte[] bodyData = Util.toByteArray(response.body().asInputStream());
            String body = bodyData.length == 0 ? EMPTY_BODY : new String(bodyData, Util.UTF_8);
            log.info(RESPONSE_MESSAGE_WITH_BODY, url, response.status(), body);
            return response.toBuilder().body(bodyData).build();
        }
        log.info(RESPONSE_MESSAGE, url);
        return response;
    }

    @Override
    protected IOException logIOException(String configKey, Level logLevel, IOException ioe, long elapsedTime) {
        log.warn(FAILED_RESPONSE_MESSAGE, configKey, ioe);
        return ioe;
    }

    @Override
    protected void log(String configKey, String format, Object... args) {
        log.debug(String.format(methodTag(configKey) + format, args));
    }
}
