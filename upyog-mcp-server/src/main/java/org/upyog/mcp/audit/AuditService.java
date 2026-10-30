package org.upyog.mcp.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.upyog.mcp.client.RequestInfoBuilder;
import org.upyog.mcp.web.AuthenticatedUser;

import java.time.Instant;
import java.util.Set;

/**
 * Writes one redacted JSON audit event per MCP tool call. Tokens, file bytes, and contact fields are removed.
 */
@Service
public class AuditService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final Set<String> REDACTED = Set.of(
            "authtoken", "password", "contentbase64", "mobilenumber", "email", "emailid",
            "applicantmobileno", "applicantemailid", "aadhaar", "aadhaarnumber");

    private final ObjectMapper objectMapper;

    public AuditService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Emits one structured audit line to the {@code AUDIT} logger.
     *
     * @param outcome success code or {@link org.upyog.mcp.service.McpException#getCode()}
     */
    public void record(AuthenticatedUser user, String service, String operation, String tenant,
                       JsonNode arguments, String outcome, long latencyMs) {
        try {
            ObjectNode event = objectMapper.createObjectNode();
            event.put("userUuid", user == null ? "" : user.getUuid());
            event.put("service", service);
            event.put("operation", operation);
            event.put("tenant", tenant == null ? "" : tenant);
            event.set("arguments", redact(arguments == null ? objectMapper.nullNode() : arguments.deepCopy()));
            event.put("outcome", outcome);
            event.put("latencyMs", latencyMs);
            event.put("correlationId", MDC.get(RequestInfoBuilder.CORRELATION_MDC));
            event.put("timestamp", Instant.now().toString());
            AUDIT.info(objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            AUDIT.info("{\"outcome\":\"audit_failed\",\"correlationId\":\"{}\"}",
                    MDC.get(RequestInfoBuilder.CORRELATION_MDC));
        }
    }

    private JsonNode redact(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.fieldNames().forEachRemaining(name -> {
                if (REDACTED.contains(name.toLowerCase())) {
                    objectNode.put(name, "[REDACTED]");
                } else {
                    objectNode.set(name, redact(objectNode.get(name)));
                }
            });
        } else if (node.isArray()) {
            node.forEach(this::redact);
        }
        return node;
    }
}
