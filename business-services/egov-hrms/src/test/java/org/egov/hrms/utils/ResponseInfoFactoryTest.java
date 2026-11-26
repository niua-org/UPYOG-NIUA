package org.egov.hrms.utils;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoFactoryTest {

    private final ResponseInfoFactory factory = new ResponseInfoFactory();

    @Test
    void createResponseInfoFromRequestInfo_successful() {
        RequestInfo requestInfo = RequestInfo.builder()
                .apiId("hrms")
                .ver("1.0")
                .msgId("msg-1")
                .ts(100L)
                .build();

        ResponseInfo responseInfo = factory.createResponseInfoFromRequestInfo(requestInfo, true);

        assertThat(responseInfo.getStatus()).isEqualTo("successful");
        assertThat(responseInfo.getApiId()).isEqualTo("hrms");
        assertThat(responseInfo.getVer()).isEqualTo("1.0");
        assertThat(responseInfo.getMsgId()).isEqualTo("msg-1");
        assertThat(responseInfo.getTs()).isEqualTo(100L);
    }

    @Test
    void createResponseInfoFromRequestInfo_failedWithNullRequest() {
        ResponseInfo responseInfo = factory.createResponseInfoFromRequestInfo(null, false);

        assertThat(responseInfo.getStatus()).isEqualTo("failed");
        assertThat(responseInfo.getApiId()).isEmpty();
    }
}
