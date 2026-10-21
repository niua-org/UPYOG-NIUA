package org.egov.vendor.utils;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoFactoryTest {

    private final ResponseInfoFactory factory = new ResponseInfoFactory();

    @Test
    @DisplayName("createResponseInfoFromRequestInfo_whenSuccess_setsStatusSuccessful")
    void createResponseInfoFromRequestInfo_whenSuccess_setsStatusSuccessful() {
        RequestInfo requestInfo = RequestInfo.builder()
                .apiId("vendor")
                .ver("1.0")
                .msgId("msg-1")
                .ts(123L)
                .build();

        ResponseInfo responseInfo = factory.createResponseInfoFromRequestInfo(requestInfo, true);

        assertThat(responseInfo.getStatus()).isEqualTo("successful");
        assertThat(responseInfo.getApiId()).isEqualTo("vendor");
        assertThat(responseInfo.getMsgId()).isEqualTo("msg-1");
    }

    @Test
    @DisplayName("createResponseInfoFromRequestInfo_whenNullRequestInfo_usesEmptyDefaults")
    void createResponseInfoFromRequestInfo_whenNullRequestInfo_usesEmptyDefaults() {
        ResponseInfo responseInfo = factory.createResponseInfoFromRequestInfo(null, false);

        assertThat(responseInfo.getStatus()).isEqualTo("failed");
        assertThat(responseInfo.getApiId()).isEmpty();
    }
}
