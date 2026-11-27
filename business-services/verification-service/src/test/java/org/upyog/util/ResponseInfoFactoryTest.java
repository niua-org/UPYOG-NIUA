package org.upyog.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoFactoryTest {

    private final ResponseInfoFactory factory = new ResponseInfoFactory();

    @Test
    void createResponseInfoFromRequestInfo_successFlag() {
        RequestInfo requestInfo = RequestInfo.builder()
                .apiId("verification")
                .ver("1.0")
                .msgId("msg-1")
                .ts(99L)
                .build();

        ResponseInfo responseInfo = factory.createResponseInfoFromRequestInfo(requestInfo, true);

        assertThat(responseInfo.getApiId()).isEqualTo("verification");
        assertThat(responseInfo.getMsgId()).isEqualTo("msg-1");
        assertThat(responseInfo.getStatus()).isEqualTo("successful");
        assertThat(responseInfo.getResMsgId()).isNotBlank();
    }

    @Test
    void createResponseInfoFromRequestInfo_failureFlag() {
        ResponseInfo responseInfo = factory.createResponseInfoFromRequestInfo(null, false);

        assertThat(responseInfo.getStatus()).isEqualTo("failed");
        assertThat(responseInfo.getApiId()).isEmpty();
    }
}
