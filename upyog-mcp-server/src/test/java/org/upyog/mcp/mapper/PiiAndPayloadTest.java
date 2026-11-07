package org.upyog.mcp.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.upyog.mcp.guard.PayloadGuard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiiAndPayloadTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void masksMobileEmailAndName() throws Exception {
        ObjectNode node = (ObjectNode) objectMapper.readTree("""
                {"applicantMobileNo":"9999999999","applicantEmailId":"abc@gmail.com","applicantName":"Ramesh"}
                """);
        ObjectNode masked = (ObjectNode) new PiiMasker().mask(node, true);
        assertEquals("******9999", masked.get("applicantMobileNo").asText());
        assertTrue(masked.get("applicantEmailId").asText().contains("***"));
        assertEquals("R***", masked.get("applicantName").asText());
    }

    @Test
    void rejectsRawRequestInfoUrlAndMethod() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> PayloadGuard.rejectForbidden(
                objectMapper.readTree("{\"RequestInfo\":{}}")));
        assertThrows(IllegalArgumentException.class, () -> PayloadGuard.rejectForbidden(
                objectMapper.readTree("{\"url\":\"https://evil.example\"}")));
        assertThrows(IllegalArgumentException.class, () -> PayloadGuard.rejectForbidden(
                objectMapper.readTree("{\"httpMethod\":\"GET\"}")));
        assertThrows(IllegalArgumentException.class, () -> PayloadGuard.rejectForbidden(
                objectMapper.readTree("{\"fileStoreId\":\"abc\"}")));
    }
}
