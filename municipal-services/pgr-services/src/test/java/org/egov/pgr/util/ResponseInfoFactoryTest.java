package org.egov.pgr.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoFactoryTest {

    private final ResponseInfoFactory factory = new ResponseInfoFactory();

    @Test
    @DisplayName("createResponseInfoFromRequestInfo_whenSuccess_setsSuccessfulStatus")
    void createResponseInfoFromRequestInfo_whenSuccess_setsSuccessfulStatus() {
        RequestInfo requestInfo = RequestInfo.builder().apiId("pgr").ver("1.0").msgId("m1").ts(1L).build();
        ResponseInfo info = factory.createResponseInfoFromRequestInfo(requestInfo, true);
        assertThat(info.getStatus()).isEqualTo("successful");
        assertThat(info.getApiId()).isEqualTo("pgr");
        assertThat(info.getTs()).isEqualTo(1L);
    }

    @Test
    @DisplayName("createResponseInfoFromRequestInfo_whenNullRequestInfo_failedStatus")
    void createResponseInfoFromRequestInfo_whenNullRequestInfo_failedStatus() {
        ResponseInfo info = factory.createResponseInfoFromRequestInfo(null, false);
        assertThat(info.getStatus()).isEqualTo("failed");
        assertThat(info.getApiId()).isEmpty();
    }
}
