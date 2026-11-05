package org.egov.ndc.config;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoFactoryTest {

    private final ResponseInfoFactory factory = new ResponseInfoFactory();

    @Test
    void createResponseInfoFromRequestInfo_whenSuccess_copiesRequestMetadata() {
        RequestInfo requestInfo = RequestInfo.builder()
                .apiId("ndc")
                .ver("1.0")
                .msgId("msg-1")
                .ts(100L)
                .build();

        ResponseInfo response = factory.createResponseInfoFromRequestInfo(requestInfo, true);

        assertThat(response.getApiId()).isEqualTo("ndc");
        assertThat(response.getStatus()).isEqualTo("successful");
        assertThat(response.getMsgId()).isEqualTo("msg-1");
    }

    @Test
    void createResponseInfoFromRequestInfo_whenFailed_setsFailedStatus() {
        ResponseInfo response = factory.createResponseInfoFromRequestInfo(null, false);

        assertThat(response.getStatus()).isEqualTo("failed");
        assertThat(response.getApiId()).isEmpty();
    }
}
