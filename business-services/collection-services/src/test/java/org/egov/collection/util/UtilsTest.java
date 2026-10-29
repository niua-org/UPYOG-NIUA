package org.egov.collection.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

    @Test
    void testIsPositiveInteger() {
        assertTrue(Utils.isPositiveInteger(BigDecimal.valueOf(42L)));
        assertTrue(Utils.isPositiveInteger(BigDecimal.valueOf(0L)));
        assertFalse(Utils.isPositiveInteger(BigDecimal.valueOf(-1L)));
    }

    @Test
    void jsonMerge_overwritesFieldsOnObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode main = mapper.createObjectNode().put("status", "OLD");
        ObjectNode update = mapper.createObjectNode().put("status", "NEW").put("count", 1);
        Utils.jsonMerge(main, update);
        assertEquals("NEW", main.get("status").asText());
        assertEquals(1, main.get("count").asInt());
    }
}

