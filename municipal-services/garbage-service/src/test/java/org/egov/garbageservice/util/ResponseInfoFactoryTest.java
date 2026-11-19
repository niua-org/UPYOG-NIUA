package org.egov.garbageservice.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoFactoryTest {

    private final ResponseInfoFactory factory = new ResponseInfoFactory();

    @Test
    void createResponseInfoFromRequestInfo_whenSuccessTrue_statusIsSuccessful() {
        RequestInfo requestInfo = RequestInfo.builder().apiId("grbg").ver("1.0").msgId("m1").build();

        ResponseInfo response = factory.createResponseInfoFromRequestInfo(requestInfo, true);

        assertThat(response.getStatus()).isEqualTo("successful");
        assertThat(response.getApiId()).isEqualTo("grbg");
    }

    @Test
    void createResponseInfoFromRequestInfo_whenSuccessFalse_statusIsFailed() {
        ResponseInfo response = factory.createResponseInfoFromRequestInfo(null, false);

        assertThat(response.getStatus()).isEqualTo("failed");
    }
}
