package org.upyog.mcp.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates a tool payload against the descriptor JSON Schema.
 * Schemas set {@code additionalProperties: false}, so unknown fields fail here.
 */
@Component
public class SchemaValidator {

    private final ObjectMapper objectMapper;
    private final SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

    public SchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param schema  descriptor {@code inputSchema}
     * @param payload MCP tool arguments after forbidden-field stripping
     */
    public void validate(JsonNode schema, JsonNode payload) {
        Schema compiled = schemaRegistry.getSchema(schema);
        List<Error> errors = compiled.validate(payload);
        if (!errors.isEmpty()) {
            String text = errors.stream().map(Error::getMessage).collect(Collectors.joining("; "));
            throw new IllegalArgumentException(text);
        }
    }

    /** Deep-copies an object node or returns an empty object when the input is not an object. */
    public ObjectNode copyObject(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            return objectNode.deepCopy();
        }
        return objectMapper.createObjectNode();
    }
}
