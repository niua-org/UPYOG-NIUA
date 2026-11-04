package org.upyog.rs.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoFactoryTest {

    private final ResponseInfoFactory factory = new ResponseInfoFactory();

    @Test
    void createResponseInfoFromRequestInfo_whenSuccess_marksSuccessful() {
        RequestInfo requestInfo = RequestInfo.builder().apiId("rs").msgId("msg").build();

        ResponseInfo response = factory.createResponseInfoFromRequestInfo(requestInfo, true);

        assertThat(response.getStatus()).isEqualTo("successful");
        assertThat(response.getApiId()).isEqualTo("rs");
    }

    @Test
    void createResponseInfoFromRequestInfo_whenNullRequestAndFailure_marksFailed() {
        ResponseInfo response = factory.createResponseInfoFromRequestInfo(null, false);

        assertThat(response.getStatus()).isEqualTo("failed");
    }
}
