package com.ingestpipeline.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseGeneratorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void failureResponse_containsFailureStatusCode() throws Exception {
        JsonNode node = objectMapper.readTree(ResponseGenerator.failureResponse());

        assertThat(node.get(JsonKey.STATUS_CODE).asText())
                .isEqualTo(String.valueOf(ResponseCode.FAILURE.getErrorCode()));
    }

    @Test
    void successResponse_wrapsPayload() throws Exception {
        JsonNode node = objectMapper.readTree(ResponseGenerator.successResponse("payload"));

        assertThat(node.get(JsonKey.STATUS).get(JsonKey.STATUS_CODE).asText())
                .isEqualTo(String.valueOf(ResponseCode.Success.getErrorCode()));
        assertThat(node.get(JsonKey.RESPONSE_DATA).asText()).isEqualTo("payload");
    }
}
