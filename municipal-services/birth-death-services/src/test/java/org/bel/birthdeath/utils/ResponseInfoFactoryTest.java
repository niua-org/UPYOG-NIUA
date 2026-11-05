package org.bel.birthdeath.utils;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoFactoryTest {

    private final ResponseInfoFactory factory = new ResponseInfoFactory();

    @Test
    void createResponseInfoFromRequestInfo_whenSuccess_returnsSuccessfulStatus() {
        RequestInfo requestInfo = RequestInfo.builder().apiId("api").msgId("m1").build();
        ResponseInfo response = factory.createResponseInfoFromRequestInfo(requestInfo, true);
        assertThat(response.getStatus()).isEqualTo("successful");
    }

    @Test
    void createResponseInfoFromRequestInfo_whenFailure_returnsFailedStatus() {
        ResponseInfo response = factory.createResponseInfoFromRequestInfo(null, false);
        assertThat(response.getStatus()).isEqualTo("failed");
    }
}
