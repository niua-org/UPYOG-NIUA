package org.upyog.mcp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EgovUserSearchResponseParserTest {

    @Test
    void readsRuntimeUserField() throws Exception {
        var node = new ObjectMapper().readTree("""
                {"user":[{"uuid":"a"}]}
                """);
        assertEquals("a", EgovUserSearchResponseParser.firstUser(node).path("uuid").asText());
    }

    @Test
    void readsSwaggerUsersField() throws Exception {
        var node = new ObjectMapper().readTree("""
                {"Users":[{"uuid":"b"}]}
                """);
        assertEquals("b", EgovUserSearchResponseParser.firstUser(node).path("uuid").asText());
    }
}
