package org.upyog.mcp.registry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.upyog.mcp.web.AuthenticatedUser;

/**
 * Fills a descriptor body from the validated payload and server context.
 * A directive node has exactly one key. Unknown {@code $} keys are rejected.
 */
@Component
public class BodyBuilder {

    private final ObjectMapper objectMapper;

    public BodyBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Resolves a descriptor request or query template into JSON using payload, pagination, and user uuid.
     *
     * @param template YAML {@code requestBody} or {@code query} node
     * @param payload  validated MCP tool input
     * @param limit    page size for {@code $page} size directive
     * @param offset   row offset for {@code $page} offset directive
     */
    public ObjectNode build(JsonNode template, JsonNode payload, AuthenticatedUser user, int limit, int offset) {
        JsonNode built = resolve(template, payload, user, limit, offset);
        if (built instanceof ObjectNode objectNode) {
            return objectNode;
        }
        return objectMapper.createObjectNode();
    }

    private JsonNode resolve(JsonNode node, JsonNode payload, AuthenticatedUser user, int limit, int offset) {
        if (node == null || node.isNull()) {
            return objectMapper.nullNode();
        }
        if (node.isObject() && isDirective(node)) {
            return directive(node, payload, user, limit, offset);
        }
        if (node.isObject()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                JsonNode value = resolve(entry.getValue(), payload, user, limit, offset);
                if (value != null && !value.isNull() && !value.isMissingNode()) {
                    objectNode.set(entry.getKey(), value);
                }
            });
            return objectNode;
        }
        if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            node.forEach(child -> arrayNode.add(resolve(child, payload, user, limit, offset)));
            return arrayNode;
        }
        return node.deepCopy();
    }

    private JsonNode directive(JsonNode node, JsonNode payload, AuthenticatedUser user, int limit, int offset) {
        if (node.has("$payload")) {
            return payload.path(node.get("$payload").asText());
        }
        if (node.has("$constant")) {
            return node.get("$constant");
        }
        if (node.has("$page")) {
            String which = node.get("$page").asText();
            return objectMapper.getNodeFactory().numberNode("offset".equals(which) ? offset : limit);
        }
        if (node.has("$user")) {
            if (!"uuid".equals(node.get("$user").asText())) {
                throw new IllegalArgumentException("Unsupported user directive");
            }
            return objectMapper.getNodeFactory().textNode(user.getUuid());
        }
        throw new IllegalArgumentException("Unsupported template directive");
    }

    private static boolean isDirective(JsonNode node) {
        if (node.size() != 1) {
            return false;
        }
        String field = node.fieldNames().next();
        return field.startsWith("$");
    }
}
