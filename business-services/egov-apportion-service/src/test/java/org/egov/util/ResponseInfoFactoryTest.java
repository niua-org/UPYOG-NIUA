package org.egov.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoFactoryTest {

    private final ResponseInfoFactory factory = new ResponseInfoFactory();

    @Test
    void createResponseInfoFromRequestInfo_successStatus() {
        RequestInfo requestInfo = RequestInfo.builder()
                .apiId("apportion")
                .msgId("msg-1")
                .build();

        ResponseInfo responseInfo = factory.createResponseInfoFromRequestInfo(requestInfo, true);

        assertThat(responseInfo.getStatus()).isEqualTo("successful");
        assertThat(responseInfo.getApiId()).isEqualTo("apportion");
    }

    @Test
    void createResponseInfoFromRequestInfo_failureStatus() {
        ResponseInfo responseInfo = factory.createResponseInfoFromRequestInfo(null, false);

        assertThat(responseInfo.getStatus()).isEqualTo("failed");
    }
}
