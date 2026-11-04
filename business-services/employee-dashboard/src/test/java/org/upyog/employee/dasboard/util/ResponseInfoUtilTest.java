package org.upyog.employee.dasboard.util;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.Test;
import org.upyog.employee.dasboard.web.models.ResponseInfo;
import org.upyog.employee.dasboard.web.models.ResponseInfo.StatusEnum;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoUtilTest {

    @Test
    void createResponseInfo_populatesFieldsFromRequest() {
        RequestInfo requestInfo = RequestInfo.builder()
                .apiId("employee-dashboard")
                .ver("1.0")
                .msgId("msg-1")
                .ts(42L)
                .build();

        ResponseInfo responseInfo = ResponseInfoUtil.createResponseInfo(
                requestInfo, "ok", StatusEnum.SUCCESSFUL);

        assertThat(responseInfo.getApiId()).isEqualTo("employee-dashboard");
        assertThat(responseInfo.getResMsgId()).isEqualTo("ok");
        assertThat(responseInfo.getStatus()).isEqualTo(StatusEnum.SUCCESSFUL);
    }
}
