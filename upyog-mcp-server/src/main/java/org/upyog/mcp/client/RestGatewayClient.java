package org.upyog.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.upyog.mcp.service.McpException;
import org.upyog.mcp.web.AuthenticatedUser;
import org.upyog.mcp.web.GatewayIdentityHeaders;
import org.upyog.mcp.web.UserContextHolder;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Gateway client with separate read and write timeouts.
 * Reads may be retried once. Writes are never retried.
 * A circuit breaker is kept per gateway path prefix (one downstream service).
 */
public class RestGatewayClient implements GatewayClient {

    private static final Logger log = LoggerFactory.getLogger(RestGatewayClient.class);

    private final RestClient readClient;
    private final RestClient writeClient;
    private final ObjectMapper objectMapper;
    private final CircuitBreakerRegistry circuitBreakers;
    private final RetryRegistry retries;

    /**
     * @param readClient  RestClient with read timeout and retry policy
     * @param writeClient RestClient with write timeout; no retries on POST writes
     */
    public RestGatewayClient(RestClient readClient, RestClient writeClient, ObjectMapper objectMapper,
                             CircuitBreakerRegistry circuitBreakers) {
        this.readClient = readClient;
        this.writeClient = writeClient;
        this.objectMapper = objectMapper;
        this.circuitBreakers = circuitBreakers;
        this.retries = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(200))
                .retryExceptions(IOException.class, DownstreamRetryException.class)
                .build());
    }

    @Override
    public JsonNode postJson(String gatewayPath, Map<String, List<String>> query, JsonNode body, boolean read) {
        return postJsonInternal(gatewayPath, query, body, read, Map.of(), true);
    }

    @Override
    public JsonNode postJson(String gatewayPath, Map<String, List<String>> query, JsonNode body, boolean read,
                             Map<String, String> headers) {
        return postJsonInternal(gatewayPath, query, body, read, headers, true);
    }

    @Override
    public JsonNode postJsonNoToken(String gatewayPath, Map<String, List<String>> query, JsonNode body, boolean read) {
        return postJsonInternal(gatewayPath, query, body, read, Map.of(), false);
    }

    private JsonNode postJsonInternal(String gatewayPath, Map<String, List<String>> query, JsonNode body,
                                      boolean read, Map<String, String> extraHeaders, boolean injectAuthToken) {
        Supplier<JsonNode> call = () -> executeJson(gatewayPath, query, body, read, extraHeaders, injectAuthToken);
        if (read) {
            call = Retry.decorateSupplier(retries.retry(serviceName(gatewayPath)), call);
        }
        try {
            return CircuitBreaker.decorateSupplier(breaker(gatewayPath), call).get();
        } catch (DownstreamRetryException exception) {
            log.warn("downstream_call path={} read={} status={} retriesExhausted=true",
                    gatewayPath, read, exception.status());
            throw new McpException("DOWNSTREAM_UNAVAILABLE", "The UPYOG service is temporarily unavailable.",
                    true, "Try the same read again shortly.");
        }
    }

    @Override
    public JsonNode postFile(String gatewayPath, String tenantId, String module, String fileName,
                             String contentType, byte[] content) {
        Supplier<JsonNode> call = () -> executeFile(gatewayPath, tenantId, module, fileName, contentType, content);
        return CircuitBreaker.decorateSupplier(breaker(gatewayPath), call).get();
    }

    private JsonNode executeJson(String gatewayPath, Map<String, List<String>> query, JsonNode body,
                                 boolean read, Map<String, String> extraHeaders, boolean injectAuthToken) {
        long started = System.nanoTime();
        try {
            RestClient client = read ? readClient : writeClient;
            RestClient.RequestBodySpec request = client.post()
                    .uri(builder -> {
                        builder.path(gatewayPath);
                        if (query != null) {
                            query.forEach((key, values) -> {
                                if (values != null) {
                                    values.stream().filter(value -> value != null && !value.isBlank())
                                            .forEach(value -> builder.queryParam(key, value));
                                }
                            });
                        }
                        return builder.build();
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-correlation-id", correlation());
            // Inject Auth-Token for gateway RBAC on downstream service calls.
            // Suppressed for /user/_search — the UI curl proves auth goes only in RequestInfo.authToken body.
            if (injectAuthToken) {
                String contextToken = currentAuthToken();
                if (contextToken != null && !contextToken.isBlank()) {
                    request = request.header(GatewayIdentityHeaders.AUTH_TOKEN, contextToken);
                }
            }
            if (extraHeaders != null) {
                for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                    String name = entry.getKey();
                    String value = entry.getValue();
                    if (name != null && !name.isBlank() && value != null && !value.isBlank()) {
                        request = request.header(name, value);
                    }
                }
            }
            String raw = request.body(body == null ? objectMapper.createObjectNode() : body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                        String payload = new String(response.getBody().readAllBytes());
                        int status = response.getStatusCode().value();
                        if (read && status >= 500) {
                            log.warn("downstream_call path={} read={} status={} latencyMs={} errorBody={} willRetry=true",
                                    gatewayPath, read, status, elapsed(started), truncate(payload));
                            throw new DownstreamRetryException(status);
                        }
                        throw new DownstreamException(status, payload);
                    })
                    .body(String.class);
            log.info("downstream_call path={} read={} status=200 latencyMs={}", gatewayPath, read, elapsed(started));
            if (raw == null || raw.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(raw);
        } catch (DownstreamException exception) {
            log.info("downstream_call path={} read={} status={} latencyMs={} errorBody={}",
                    gatewayPath, read, exception.status(), elapsed(started), truncate(exception.body()));
            throw mapStatus(exception.status(), exception.body());
        } catch (DownstreamRetryException exception) {
            throw exception;
        } catch (CallNotPermittedException exception) {
            throw new McpException("CIRCUIT_OPEN", "The UPYOG service is temporarily unavailable.",
                    true, "Try the same read again shortly.");
        } catch (McpException exception) {
            throw exception;
        } catch (Exception exception) {
            log.info("downstream_call path={} read={} status=error latencyMs={}", gatewayPath, read, elapsed(started));
            throw new McpException("DOWNSTREAM_UNAVAILABLE", "The UPYOG service could not be reached.",
                    read, read ? "Try the same read again shortly." : "Prepare the action again. Do not retry the write yourself.");
        }
    }

    private JsonNode executeFile(String gatewayPath, String tenantId, String module, String fileName,
                                 String contentType, byte[] content) {
        LinkedMultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        try {
            String raw = writeClient.post()
                    .uri(builder -> builder.path(gatewayPath)
                            .queryParam("tenantId", tenantId)
                            .queryParam("module", module)
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("x-correlation-id", correlation())
                    .body(multipart)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new DownstreamException(response.getStatusCode().value(), "");
                    })
                    .body(String.class);
            return raw == null ? objectMapper.createObjectNode() : objectMapper.readTree(raw);
        } catch (DownstreamException exception) {
            throw new McpException("FILE_UPLOAD_FAILED", "The document could not be stored.",
                    false, "Try the upload again with a smaller file, or continue in the UPYOG UI.");
        } catch (Exception exception) {
            throw new McpException("FILE_UPLOAD_FAILED", "The document could not be stored.",
                    true, "Try the upload again.");
        }
    }

    private McpException mapStatus(int status, String body) {
        String safe = safeMessage(body);
        if (status == 401 || status == 403) {
            return new McpException("NOT_AUTHORIZED", "You are not allowed to perform this action.",
                    false, "Use an account that has access to this service and tenant.");
        }
        if (status == 404) {
            return new McpException("NOT_FOUND", "No matching record was found.",
                    false, "Check the identifier and tenant.");
        }
        if (status == 429) {
            return new McpException("RATE_LIMITED", "The UPYOG service is busy.",
                    true, "Wait and try the read again.");
        }
        if (status >= 500) {
            return new McpException("DOWNSTREAM_UNAVAILABLE", "The UPYOG service is temporarily unavailable.",
                    true, "Try the same read again shortly.");
        }
        return new McpException("DOWNSTREAM_REJECTED", safe,
                false, "Correct the business details and prepare the action again.");
    }

    private String safeMessage(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode errors = root.get("Errors");
            if (errors != null && errors.isArray() && !errors.isEmpty()) {
                String message = errors.get(0).path("message").asText("");
                if (message.length() <= 240 && !message.contains("Exception") && !message.contains("http")) {
                    return message;
                }
            }
        } catch (Exception ignored) {
            return "The UPYOG service rejected the request.";
        }
        return "The UPYOG service rejected the request.";
    }

    private CircuitBreaker breaker(String path) {
        return circuitBreakers.circuitBreaker(serviceName(path));
    }

    private static String serviceName(String path) {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        int slash = trimmed.indexOf('/');
        return slash < 0 ? trimmed : trimmed.substring(0, slash);
    }

    private static String correlation() {
        String value = MDC.get(RequestInfoBuilder.CORRELATION_MDC);
        return value == null ? "" : value;
    }

    private static long elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    /** Reads the auth token from the current request's authenticated user context. */
    private static String currentAuthToken() {
        AuthenticatedUser user = UserContextHolder.get();
        return user != null ? user.getAuthToken() : null;
    }

    private static String truncate(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String singleLine = body.replaceAll("\\s+", " ");
        return singleLine.length() <= 500 ? singleLine : singleLine.substring(0, 500) + "…";
    }

    private static final class DownstreamException extends RuntimeException {
        private final int status;
        private final String body;

        private DownstreamException(int status, String body) {
            this.status = status;
            this.body = body;
        }

        private int status() {
            return status;
        }

        private String body() {
            return body;
        }
    }

    private static final class DownstreamRetryException extends RuntimeException {
        private final int status;

        private DownstreamRetryException(int status) {
            super(Integer.toString(status));
            this.status = status;
        }

        private int status() {
            return status;
        }
    }
}
