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

    @Test
    void findValue_readsNestedFieldCaseInsensitive() {
        ObjectNode parent = objectMapper.createObjectNode();
        ObjectNode child = objectMapper.createObjectNode();
        child.put("TenantId", "pb.amritsar");
        parent.set("data", child);

        StringBuffer value = new StringBuffer();
        JSONUtil.findValue(parent, "tenantid", value);

        assertThat(value.toString()).isEqualTo("pb.amritsar");
    }

    @Test
    void replaceFieldValue_updatesNestedArrayObjects() {
        ObjectNode parent = objectMapper.createObjectNode();
        ObjectNode item = objectMapper.createObjectNode();
        item.put("status", "OLD");
        parent.putArray("items").add(item);

        JSONUtil.replaceFieldValue(parent, "status", "NEW");

        assertThat(parent.get("items").get(0).get("status").asText()).isEqualTo("NEW");
    }
}
