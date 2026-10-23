package com.ingestpipeline.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JSONUtilTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void replaceFieldValue_updatesExistingField() {
        ObjectNode parent = objectMapper.createObjectNode();
        parent.put("name", "old");

        JSONUtil.replaceFieldValue(parent, "name", "new");

        assertThat(parent.get("name").asText()).isEqualTo("new");
    }

    @Test
    void replaceFieldValue_ignoresMissingField() {
        ObjectNode parent = objectMapper.createObjectNode();

        JSONUtil.replaceFieldValue(parent, "missing", "value");

        assertThat(parent.has("missing")).isFalse();
    }
}
