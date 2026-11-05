package org.egov.infra.indexer.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoFactoryTest {

    private final ResponseInfoFactory factory = new ResponseInfoFactory();

    @Test
    void createResponseInfoFromRequestInfo_success_copiesMetadataAndStatus() {
        RequestInfo requestInfo = RequestInfo.builder()
                .apiId("indexer")
                .ver("1.0")
                .msgId("msg-1")
                .ts(123L)
                .build();

        ResponseInfo response = factory.createResponseInfoFromRequestInfo(requestInfo, true);

        assertThat(response.getApiId()).isEqualTo("indexer");
        assertThat(response.getVer()).isEqualTo("1.0");
        assertThat(response.getMsgId()).isEqualTo("msg-1");
        assertThat(response.getTs()).isEqualTo(123L);
        assertThat(response.getStatus()).isEqualTo("successful");
    }

    @Test
    void createResponseInfoFromRequestInfo_failure_setsFailedStatus() {
        ResponseInfo response = factory.createResponseInfoFromRequestInfo(null, false);

        assertThat(response.getStatus()).isEqualTo("failed");
        assertThat(response.getApiId()).isEmpty();
    }
}
