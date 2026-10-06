package org.egov.externalaudit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.externalaudit.config.ExternalApiAuditProperties;
import org.egov.externalaudit.constants.ExternalApiAuditConstants;
import org.egov.externalaudit.masking.SensitivePayloadMasker;
import org.egov.externalaudit.model.ExternalApiAuditDetail;
import org.egov.externalaudit.model.ExternalApiAuditDetailWrapper;
import org.egov.externalaudit.model.ExternalApiErrorDetails;
import org.egov.externalaudit.model.ExternalIntegrationContext;
import org.egov.externalaudit.producer.ExternalApiAuditProducer;
import org.egov.externalaudit.producer.ExternalApiAuditPublisher;
import org.egov.tracer.constants.TracerConstants;
import org.egov.tracer.model.CustomException;
import org.egov.tracer.model.ServiceCallException;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Reusable executor for explicitly identified EXTERNAL integrations.
 * Publishes two Kafka events (INITIATED, then SUCCESS/FAILED) that upsert one audit row.
 * Kafka publish is async and fail-open. The original business exception is always rethrown.
 */
@Slf4j
@Component
@ConditionalOnBean(ExternalApiAuditProducer.class)
public class ExternalApiAuditLogger {

    private final ExternalApiAuditPublisher producer;
    private final ExternalApiAuditProperties properties;
    private final ObjectMapper objectMapper;
    private final SensitivePayloadMasker sensitivePayloadMasker;

    public ExternalApiAuditLogger(ExternalApiAuditPublisher producer,
            ExternalApiAuditProperties properties,
            ObjectMapper objectMapper,
            SensitivePayloadMasker sensitivePayloadMasker) {
        this.producer = producer;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.sensitivePayloadMasker = sensitivePayloadMasker;
    }

    public <T> T logInboundApi(ExternalIntegrationContext context, Supplier<T> apiCall) {
        context.setDirection(ExternalApiAuditConstants.DIRECTION_INBOUND);
        return execute(context, apiCall);
    }

    public <T> T logAndExecute(ExternalIntegrationContext context, Supplier<T> apiCall) {
        context.setDirection(ExternalApiAuditConstants.DIRECTION_OUTBOUND);
        return execute(context, apiCall);
    }

    /**
     * Backward-compatible inbound wrapper used by national-dashboard-ingest.
     * A new audit correlation id is generated; {@code correlationId} is kept as originating id.
     */
    public <T> T logInboundApi(String correlationId, String tenantId, String externalApiName,
            Object requestPayload, Supplier<T> apiCall) {
        return logInboundApi(ExternalIntegrationContext.builder()
                .originatingCorrelationId(correlationId)
                .tenantId(tenantId)
                .externalApiName(externalApiName)
                .requestPayload(requestPayload)
                .build(), apiCall);
    }

    public <T> T logAndExecute(String correlationId, String tenantId, String externalApiName,
            Object requestPayload, Supplier<T> apiCall) {
        return logAndExecute(ExternalIntegrationContext.builder()
                .originatingCorrelationId(correlationId)
                .tenantId(tenantId)
                .externalApiName(externalApiName)
                .requestPayload(requestPayload)
                .build(), apiCall);
    }

    public <T> T execute(ExternalIntegrationContext context, Supplier<T> apiCall) {
        String correlationId = resolveAuditCorrelationId(context);
        context.setCorrelationId(correlationId);
        applyDefaults(context);

        long requestTime = System.currentTimeMillis();
        publishRequestEvent(context, requestTime);

        T response = null;
        Exception caughtException = null;
        try {
            response = apiCall.get();
            return response;
        } catch (Exception exception) {
            caughtException = exception;
            throw exception;
        } finally {
            publishResponseEvent(context, requestTime, response, caughtException);
        }
    }

    private void applyDefaults(ExternalIntegrationContext context) {
        if (context.getRetryCount() == null) {
            context.setRetryCount(0);
        }
        if (isBlank(context.getState())) {
            context.setState(properties.getSourceService());
        }
        if (isBlank(context.getTenantId())) {
            context.setTenantId(ExternalApiAuditConstants.DEFAULT_TENANT);
        }
        if (isBlank(context.getOriginatingCorrelationId())) {
            String mdcCorrelationId = MDC.get(TracerConstants.CORRELATION_ID_MDC);
            if (!isBlank(mdcCorrelationId)) {
                context.setOriginatingCorrelationId(mdcCorrelationId);
            }
        }
    }

    private String resolveAuditCorrelationId(ExternalIntegrationContext context) {
        if (context.getCorrelationId() != null && !context.getCorrelationId().isBlank()) {
            return context.getCorrelationId();
        }
        return UUID.randomUUID().toString();
    }

    private void publishRequestEvent(ExternalIntegrationContext context, long requestTime) {
        long currentTime = System.currentTimeMillis();
        PayloadResult payloadResult = preparePayload(buildAuditablePayload(context, context.getRequestPayload()));

        ExternalApiAuditDetail requestEvent = ExternalApiAuditDetail.builder()
                .id(context.getCorrelationId())
                .rawDetailId(UUID.randomUUID().toString())
                .correlationId(context.getCorrelationId())
                .tenantId(context.getTenantId())
                .state(context.getState())
                .externalApiName(context.getExternalApiName())
                .direction(context.getDirection())
                .requestTime(requestTime)
                .status(ExternalApiAuditConstants.STATUS_INITIATED)
                .retryCount(context.getRetryCount())
                .createdTime(currentTime)
                .lastModifiedTime(currentTime)
                .requestPayload(payloadResult.payload())
                .payloadSizeBytes(payloadResult.sizeBytes())
                .build();

        publishSafely(requestEvent);
    }

    private void publishResponseEvent(ExternalIntegrationContext context, long requestTime, Object response,
            Exception caughtException) {
        long responseTime = System.currentTimeMillis();
        long durationMs = responseTime - requestTime;
        ResponseAuditDetails auditDetails = buildResponseAuditDetails(context.getCorrelationId(), response, caughtException);
        PayloadResult requestPayloadResult = preparePayload(buildAuditablePayload(context, context.getRequestPayload()));
        PayloadResult responsePayloadResult = preparePayload(buildAuditablePayload(context, auditDetails.responsePayload()));

        ExternalApiAuditDetail responseEvent = ExternalApiAuditDetail.builder()
                .id(context.getCorrelationId())
                .correlationId(context.getCorrelationId())
                .tenantId(context.getTenantId())
                .state(context.getState())
                .externalApiName(context.getExternalApiName())
                .direction(context.getDirection())
                .requestTime(requestTime)
                .createdTime(requestTime)
                .status(auditDetails.status())
                .httpStatusCode(auditDetails.httpStatusCode())
                .responseTime(responseTime)
                .durationMs(durationMs)
                .retryCount(context.getRetryCount())
                .lastModifiedTime(responseTime)
                .requestPayload(requestPayloadResult.payload())
                .responsePayload(responsePayloadResult.payload())
                .payloadSizeBytes(responsePayloadResult.sizeBytes())
                .errorDetails(auditDetails.errorDetails())
                .build();

        publishSafely(responseEvent);
    }

    private void publishSafely(ExternalApiAuditDetail event) {
        try {
            producer.publishAsync(properties.getDetailTopic(),
                    ExternalApiAuditDetailWrapper.builder().apiAuditDetail(event).build());
        } catch (Exception exception) {
            log.error("Failed to publish integration audit event to topic {}: {}",
                    properties.getDetailTopic(), exception.getMessage());
        }
    }

    private Object buildAuditablePayload(ExternalIntegrationContext context, Object rawPayload) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        putIfPresent(envelope, "originatingCorrelationId", context.getOriginatingCorrelationId());
        putIfPresent(envelope, "businessReferenceId", context.getBusinessReferenceId());
        putIfPresent(envelope, "endpoint", context.getEndpoint());
        putIfPresent(envelope, "httpMethod", context.getHttpMethod());
        envelope.put("retryCount", context.getRetryCount());
        if (properties.isCapturePayloadEnabled()) {
            envelope.put("payload", sensitivePayloadMasker.mask(rawPayload));
        } else {
            envelope.put("payloadCaptured", false);
        }
        return envelope;
    }

    private void putIfPresent(Map<String, Object> envelope, String key, String value) {
        if (!isBlank(value)) {
            envelope.put(key, value);
        }
    }

    private ResponseAuditDetails buildResponseAuditDetails(String correlationId, Object response,
            Exception caughtException) {
        if (caughtException == null) {
            return new ResponseAuditDetails(
                    ExternalApiAuditConstants.STATUS_SUCCESS,
                    extractSuccessHttpStatus(response),
                    extractSuccessBody(response),
                    null);
        }

        ExternalApiErrorDetails errorDetails = buildErrorDetails(caughtException);
        errorDetails.setCorrelationId(correlationId);
        return new ResponseAuditDetails(
                ExternalApiAuditConstants.STATUS_FAILED,
                extractHttpStatusCode(caughtException),
                extractErrorResponseBody(caughtException),
                errorDetails);
    }

    private Integer extractSuccessHttpStatus(Object response) {
        if (response instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getStatusCode().value();
        }
        return response != null ? 200 : null;
    }

    private Object extractSuccessBody(Object response) {
        if (response instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getBody();
        }
        return response;
    }

    private ExternalApiErrorDetails buildErrorDetails(Exception exception) {
        String errorCode = "INTEGRATION_CALL_FAILED";
        String errorType = ExternalApiAuditConstants.ERROR_TYPE_SERVER;
        String errorMessage = exception.getMessage();

        if (exception instanceof HttpClientErrorException clientErrorException) {
            errorType = ExternalApiAuditConstants.ERROR_TYPE_CLIENT;
            errorCode = "HTTP_CLIENT_ERROR";
            errorMessage = clientErrorException.getResponseBodyAsString();
        } else if (exception instanceof HttpServerErrorException serverErrorException) {
            errorType = ExternalApiAuditConstants.ERROR_TYPE_SERVER;
            errorCode = "HTTP_SERVER_ERROR";
            errorMessage = serverErrorException.getResponseBodyAsString();
        } else if (exception instanceof ResourceAccessException) {
            errorType = isTimeoutException(exception)
                    ? ExternalApiAuditConstants.ERROR_TYPE_TIMEOUT
                    : ExternalApiAuditConstants.ERROR_TYPE_NETWORK;
            errorCode = isTimeoutException(exception) ? "REQUEST_TIMEOUT" : "NETWORK_ERROR";
        } else if (isFeignException(exception)) {
            Integer status = invokeIntegerMethod(exception, "status");
            if (status != null && status >= 400 && status < 500) {
                errorType = ExternalApiAuditConstants.ERROR_TYPE_CLIENT;
                errorCode = "HTTP_CLIENT_ERROR";
            } else if (status != null && status >= 500) {
                errorType = ExternalApiAuditConstants.ERROR_TYPE_SERVER;
                errorCode = "HTTP_SERVER_ERROR";
            }
            String feignBody = invokeStringMethod(exception, "contentUTF8");
            if (!isBlank(feignBody)) {
                errorMessage = feignBody;
            }
        } else if (exception instanceof ServiceCallException) {
            errorType = ExternalApiAuditConstants.ERROR_TYPE_CLIENT;
            errorCode = "SERVICE_CALL_ERROR";
        } else if (exception instanceof CustomException customException) {
            errorType = ExternalApiAuditConstants.ERROR_TYPE_VALIDATION;
            errorCode = customException.getCode();
            errorMessage = customException.getMessage();
        }

        return ExternalApiErrorDetails.builder()
                .id(UUID.randomUUID().toString())
                .correlationId(null)
                .errorCode(errorCode)
                .errorType(errorType)
                .errorMessage(errorMessage)
                .createdTime(System.currentTimeMillis())
                .build();
    }

    private boolean isTimeoutException(Exception exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return exception.getMessage() != null && exception.getMessage().toLowerCase().contains("timed out");
    }

    private Integer extractHttpStatusCode(Exception exception) {
        if (exception instanceof HttpClientErrorException clientErrorException) {
            return clientErrorException.getStatusCode().value();
        }
        if (exception instanceof HttpServerErrorException serverErrorException) {
            return serverErrorException.getStatusCode().value();
        }
        Integer feignStatus = invokeIntegerMethod(exception, "status");
        if (isFeignException(exception) && feignStatus != null) {
            return feignStatus;
        }
        return 4094;
    }

    private Object extractErrorResponseBody(Exception exception) {
        if (exception instanceof HttpClientErrorException clientErrorException) {
            return clientErrorException.getResponseBodyAsString();
        }
        if (exception instanceof HttpServerErrorException serverErrorException) {
            return serverErrorException.getResponseBodyAsString();
        }
        if (isFeignException(exception)) {
            String feignBody = invokeStringMethod(exception, "contentUTF8");
            if (!isBlank(feignBody)) {
                return feignBody;
            }
        }
        if (exception instanceof ServiceCallException serviceCallException) {
            return serviceCallException.getMessage();
        }
        return exception.getMessage();
    }

    private boolean isFeignException(Exception exception) {
        Class<?> type = exception.getClass();
        while (type != null) {
            if ("feign.FeignException".equals(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private Integer invokeIntegerMethod(Exception exception, String methodName) {
        try {
            Method method = exception.getClass().getMethod(methodName);
            Object value = method.invoke(exception);
            if (value instanceof Integer integerValue) {
                return integerValue;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String invokeStringMethod(Exception exception, String methodName) {
        try {
            Method method = exception.getClass().getMethod(methodName);
            Object value = method.invoke(exception);
            return value != null ? value.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private PayloadResult preparePayload(Object payload) {
        if (payload == null) {
            return new PayloadResult(null, 0L);
        }
        try {
            byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
            long payloadSizeBytes = payloadBytes.length;
            if (payloadSizeBytes <= properties.getMaxPayloadBytes()) {
                return new PayloadResult(payload, payloadSizeBytes);
            }
            Map<String, Object> truncatedPayload = new LinkedHashMap<>();
            truncatedPayload.put("truncated", true);
            truncatedPayload.put("originalSizeBytes", payloadSizeBytes);
            truncatedPayload.put("message", ExternalApiAuditConstants.PAYLOAD_TRUNCATED_MESSAGE);
            return new PayloadResult(truncatedPayload, payloadSizeBytes);
        } catch (Exception exception) {
            log.warn("Unable to serialize integration audit payload: {}", exception.getMessage());
            return new PayloadResult(payload, 0L);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ResponseAuditDetails(String status, Integer httpStatusCode, Object responsePayload,
            ExternalApiErrorDetails errorDetails) {
    }

    private record PayloadResult(Object payload, Long sizeBytes) {
    }
}
