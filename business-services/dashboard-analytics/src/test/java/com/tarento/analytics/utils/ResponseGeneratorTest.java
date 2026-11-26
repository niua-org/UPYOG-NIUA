package com.tarento.analytics.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseGeneratorTest {

    @Test
    void failureResponse_defaultShape() throws Exception {
        String json = ResponseGenerator.failureResponse();
        assertThat(json).contains("\"statusCode\"");
        assertThat(json).contains(String.valueOf(ResponseCode.FAILURE.getErrorCode()));
    }

    @Test
    void failureResponse_withMessage_wrapsStatus() throws Exception {
        String json = ResponseGenerator.failureResponse("bad input");
        assertThat(json).contains("bad input");
        assertThat(json).contains("statusInfo");
    }

    @Test
    void failureResponse_withCodeAndMessage() throws Exception {
        String json = ResponseGenerator.failureResponse("ERR", "details");
        assertThat(json).contains("details");
    }

    @Test
    void successResponse_withObject_includesResponseData() throws Exception {
        String json = ResponseGenerator.successResponse(Map.of("k", "v"));
        assertThat(json).contains(String.valueOf(ResponseCode.Success.getErrorCode()));
        assertThat(json).contains("responseData");
        assertThat(json).contains("\"k\":\"v\"");
    }

    @Test
    void successResponse_withMessage() throws Exception {
        String json = ResponseGenerator.successResponse("done");
        assertThat(json).contains("done");
    }
}
