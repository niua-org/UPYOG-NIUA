package org.upyog.mcp.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import org.springframework.stereotype.Component;
import org.upyog.mcp.config.McpProperties;

import java.util.List;

/**
 * Projects a downstream JSON document down to the fields named in the descriptor,
 * then masks PII. Citizen free text that remains is marked {@code untrustedData}.
 */
@Component
public class ResponseProjector {

    private final ObjectMapper objectMapper;
    private final PiiMasker piiMasker;
    private final McpProperties properties;

    public ResponseProjector(ObjectMapper objectMapper, PiiMasker piiMasker, McpProperties properties) {
        this.objectMapper = objectMapper;
        this.piiMasker = piiMasker;
        this.properties = properties;
    }

    /**
     * Projects downstream JSON to descriptor {@code response.keep} fields and applies PII masking.
     *
     * @param responseSpec descriptor {@code response} node; may be null to pass through masked data
     */
    public ObjectNode project(JsonNode downstream, JsonNode responseSpec) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("untrustedData", true);
        if (responseSpec == null || responseSpec.isNull()) {
            result.set("data", piiMasker.mask(downstream.deepCopy(), true));
            return limit(result);
        }
        String listPath = responseSpec.path("listPath").asText("");
        JsonNode source = listPath.isBlank() ? downstream : readPath(downstream, listPath);
        ArrayNode projected = objectMapper.createArrayNode();
        if (source != null && source.isArray()) {
            source.forEach(item -> projected.add(keep(item, responseSpec.get("keep"))));
        } else if (source != null && !source.isMissingNode()) {
            projected.add(keep(source, responseSpec.get("keep")));
        }
        result.set("items", piiMasker.mask(projected, true));
        if (downstream.has("count")) {
            result.set("count", downstream.get("count"));
        }
        return limit(result);
    }

    private JsonNode keep(JsonNode item, JsonNode keep) {
        if (keep == null || !keep.isArray()) {
            return item.deepCopy();
        }
        ObjectNode objectNode = objectMapper.createObjectNode();
        for (JsonNode path : keep) {
            String text = path.asText();
            JsonNode value = item.at(toPointer(text));
            if (!value.isMissingNode()) {
                putPointer(objectNode, text, value.deepCopy());
            }
        }
        return objectNode;
    }

    private JsonNode readPath(JsonNode root, String path) {
        try {
            Object value = JsonPath.read(objectMapper.writeValueAsString(root), "$." + path);
            return objectMapper.valueToTree(value);
        } catch (Exception exception) {
            return objectMapper.nullNode();
        }
    }

    private static String toPointer(String dotted) {
        return "/" + dotted.replace(".", "/");
    }

    private void putPointer(ObjectNode root, String dotted, JsonNode value) {
        String[] parts = dotted.split("\\.");
        ObjectNode current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonNode next = current.get(parts[i]);
            if (!(next instanceof ObjectNode)) {
                next = objectMapper.createObjectNode();
                current.set(parts[i], next);
            }
            current = (ObjectNode) next;
        }
        current.set(parts[parts.length - 1], value);
    }

    private ObjectNode limit(ObjectNode result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            if (json.length() > properties.getMaxResponseChars()) {
                ObjectNode truncated = objectMapper.createObjectNode();
                truncated.put("truncated", true);
                truncated.put("untrustedData", true);
                truncated.put("message", "The response was truncated to the configured size limit.");
                return truncated;
            }
        } catch (Exception ignored) {
            return result;
        }
        return result;
    }

    /** Reads a dotted status field from a projected item, or returns null when missing. */
    public String statusOf(JsonNode item, String statusField) {
        if (statusField == null || statusField.isBlank()) {
            return null;
        }
        JsonNode value = item.at(toPointer(statusField));
        return value.isMissingNode() ? null : value.asText();
    }

    public List<String> textList(JsonNode array) {
        if (array == null || !array.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(array, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }
}
