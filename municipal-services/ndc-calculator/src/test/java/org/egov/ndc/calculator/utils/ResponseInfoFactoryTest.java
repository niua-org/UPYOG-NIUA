package org.egov.ndc.calculator.utils;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoFactoryTest {

    @Test
    void createResponseInfoFromRequestInfo_copiesRequestFields() {
        RequestInfo requestInfo = RequestInfo.builder().apiId("ndc").ver("1.0").msgId("m1").ts(42L).build();

        ResponseInfo response = ResponseInfoFactory.createResponseInfoFromRequestInfo(requestInfo);

        assertThat(response.getApiId()).isEqualTo("ndc");
        assertThat(response.getMsgId()).isEqualTo("m1");
        assertThat(response.getTs()).isEqualTo(42L);
        assertThat(response.getResMsgId()).isEqualTo("uief87324");
    }

    @Test
    void createResponseInfoFromRequestInfo_whenNull_usesEmptyStrings() {
        ResponseInfo response = ResponseInfoFactory.createResponseInfoFromRequestInfo(null);

        assertThat(response.getApiId()).isEmpty();
        assertThat(response.getMsgId()).isEmpty();
    }
}
