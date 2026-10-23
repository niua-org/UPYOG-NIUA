package org.upyog.util;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.Test;
import org.upyog.web.models.ResponseInfo;
import org.upyog.web.models.ResponseInfo.StatusEnum;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CommonDetailUtilTest {

    @Test
    void createReponseInfo_copiesRequestMetadata() {
        RequestInfo requestInfo = RequestInfo.builder()
                .apiId("verification")
                .ver("1.0")
                .msgId("msg-1")
                .ts(123L)
                .build();

        ResponseInfo responseInfo = CommonDetailUtil.createReponseInfo(requestInfo, "res-1", StatusEnum.SUCCESSFUL);

        assertThat(responseInfo.getApiId()).isEqualTo("verification");
        assertThat(responseInfo.getVer()).isEqualTo("1.0");
        assertThat(responseInfo.getMsgId()).isEqualTo("msg-1");
        assertThat(responseInfo.getTs()).isEqualTo(123L);
        assertThat(responseInfo.getResMsgId()).isEqualTo("res-1");
        assertThat(responseInfo.getStatus()).isEqualTo(StatusEnum.SUCCESSFUL);
    }

    @Test
    void normalizeCommaSeparatedAddress_trimsDuplicateCommas() {
        assertThat(CommonDetailUtil.normalizeCommaSeparatedAddress(",line1,, line2,"))
                .isEqualTo("line1, line2");
    }

    @Test
    void maskMobileNumber_masksMiddleDigits() {
        assertThat(CommonDetailUtil.maskMobileNumber("9999007890")).isEqualTo("999****890");
        assertThat(CommonDetailUtil.maskMobileNumber("123")).isEqualTo("NA");
    }

    @Test
    void parseLocalDateToString_usesDefaultFormat() {
        assertThat(CommonDetailUtil.parseLocalDateToString(LocalDate.of(2024, 6, 1), null))
                .isEqualTo("2024-06-01");
    }

    @Test
    void getTenantId_returnsStateLevelTenant() {
        assertThat(CommonDetailUtil.getTenantId("pb.amritsar")).isEqualTo("pb");
    }
}
