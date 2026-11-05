package org.egov.pg.utils;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pg.web.models.ResponseInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ResponseInfoFactoryTest {

    @Test
    public void createResponseInfoFromRequestInfoSuccess() {
        RequestInfo requestInfo = RequestInfo.builder()
                .apiId("api")
                .ver("1.0")
                .msgId("msg")
                .ts(123L)
                .build();

        ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);

        assertEquals("api", responseInfo.getApiId());
        assertEquals(ResponseInfo.StatusEnum.SUCCESSFUL, responseInfo.getStatus());
        assertEquals(Long.valueOf(123L), responseInfo.getTs());
    }

    @Test
    public void createResponseInfoFromNullRequestInfo() {
        ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(null, false);

        assertEquals("", responseInfo.getApiId());
        assertEquals(ResponseInfo.StatusEnum.FAILED, responseInfo.getStatus());
        assertNull(responseInfo.getTs());
    }
}
