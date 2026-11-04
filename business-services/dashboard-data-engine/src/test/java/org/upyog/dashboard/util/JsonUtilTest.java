package org.upyog.dashboard.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class JsonUtilTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("toJsonString returns empty object for blank input")
    void toJsonString_blankInput_returnsEmptyObject() {
        assertThat(JsonUtil.toJsonString(null, objectMapper)).isEqualTo("{}");
        assertThat(JsonUtil.toJsonString("  ", objectMapper)).isEqualTo("{}");
    }

    @Test
    @DisplayName("toJsonString preserves valid JSON object input")
    void toJsonString_validJsonObject_returnsSameString() {
        String json = "{\"status\":\"SUCCESS\"}";
        assertThat(JsonUtil.toJsonString(json, objectMapper)).isEqualTo(json);
    }

    @Test
    @DisplayName("toJsonString wraps non-JSON text in error object")
    void toJsonString_plainText_wrapsAsErrorJson() throws Exception {
        String result = JsonUtil.toJsonString("connection timeout", objectMapper);
        assertThat(objectMapper.readTree(result).get("error").asText()).isEqualTo("connection timeout");
    }
}
