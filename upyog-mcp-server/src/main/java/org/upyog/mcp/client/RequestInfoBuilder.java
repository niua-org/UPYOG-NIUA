package org.upyog.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.MDC;
import org.upyog.mcp.config.McpProperties;
import org.upyog.mcp.web.AuthenticatedUser;

import java.time.Instant;
import java.util.UUID;

/**
 * Builds the UPYOG {@code RequestInfo} object on the server.
 * The assistant is not allowed to supply this object, the auth token, or {@code userInfo}.
 * The gateway overwrites {@code userInfo} after it validates the token.
 */
public class RequestInfoBuilder {

    /** MDC key populated by {@link org.upyog.mcp.observability.CorrelationIdFilter}. */
    public static final String CORRELATION_MDC = "CORRELATION_ID";

    private final ObjectMapper objectMapper;
    private final McpProperties properties;

    public RequestInfoBuilder(ObjectMapper objectMapper, McpProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /** Builds {@code RequestInfo} with apiId, timestamp, msgId, auth token, and optional correlation id. */
    public ObjectNode build(AuthenticatedUser user) {
        return build(user.getAuthToken());
    }

    /**
     * Builds {@code RequestInfo} for a session token (inbound MCP auth and all gateway POST bodies).
     * Matches citizen UI shape: {@code apiId}, {@code authToken}, {@code plainAccessRequest}.
     */
    public ObjectNode build(String authToken) {
        ObjectNode info = objectMapper.createObjectNode();
        info.put("apiId", properties.getRequestInfoApiId());
        info.put("ver", "1.0");
        info.put("ts", Instant.now().toEpochMilli());
        info.put("msgId", UUID.randomUUID().toString());
        info.put("authToken", authToken);
        info.set("plainAccessRequest", objectMapper.createObjectNode());
        String correlationId = MDC.get(CORRELATION_MDC);
        if (correlationId != null && !correlationId.isBlank()) {
            info.put("correlationId", correlationId);
        }
        return info;
    }

    /** Wraps {@link #build(String)} as the sole top-level {@code RequestInfo} key. */
    public ObjectNode sessionRequestBody(String authToken) {
        ObjectNode root = objectMapper.createObjectNode();
        root.set("RequestInfo", build(authToken));
        return root;
    }

    /** Merges {@code RequestInfo} with top-level business keys expected by UPYOG APIs. */
    public ObjectNode wrap(AuthenticatedUser user, ObjectNode businessBody) {
        ObjectNode root = objectMapper.createObjectNode();
        root.set("RequestInfo", build(user));
        if (businessBody != null) {
            businessBody.fields().forEachRemaining(entry -> root.set(entry.getKey(), entry.getValue()));
        }
        return root;
    }

    /** @return correlation id from MDC, or empty when unset */
    public static String correlationId() {
        String current = MDC.get(CORRELATION_MDC);
        return current == null ? "" : current;
    }

    /** Returns a copy of a gateway request with {@code authToken} redacted for logs. */
    public JsonNode withoutSecrets(ObjectNode request) {
        ObjectNode copy = request.deepCopy();
        JsonNode info = copy.get("RequestInfo");
        if (info instanceof ObjectNode objectNode) {
            objectNode.put("authToken", "[REDACTED]");
            objectNode.remove("userInfo");
        }
        return copy;
    }
}
