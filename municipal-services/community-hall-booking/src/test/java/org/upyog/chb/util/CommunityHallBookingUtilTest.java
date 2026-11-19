package org.upyog.chb.util;

import org.egov.common.contract.request.RequestInfo;
import org.upyog.chb.web.models.AuditDetails;
import org.upyog.chb.web.models.ResponseInfo;
import org.upyog.chb.web.models.ResponseInfo.StatusEnum;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityHallBookingUtilTest {

    @Test
    void createReponseInfo_whenRequestPresent_copiesMetadata() {
        RequestInfo requestInfo = RequestInfo.builder().apiId("chb").ver("1.0").msgId("msg-1").build();

        ResponseInfo response = CommunityHallBookingUtil.createReponseInfo(requestInfo, "res-1", StatusEnum.SUCCESSFUL);

        assertThat(response.getApiId()).isEqualTo("chb");
        assertThat(response.getMsgId()).isEqualTo("msg-1");
        assertThat(response.getStatus()).isEqualTo(StatusEnum.SUCCESSFUL);
        assertThat(response.getResMsgId()).isEqualTo("res-1");
    }

    @Test
    void getAuditDetails_whenCreate_populatesCreatedAndModified() {
        AuditDetails audit = CommunityHallBookingUtil.getAuditDetails("user-1", true);

        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-1");
        assertThat(audit.getCreatedTime()).isNotNull();
    }

    @Test
    void parseStringToLocalDate_parsesConfiguredFormat() {
        LocalDate date = CommunityHallBookingUtil.parseStringToLocalDate("2026-03-01");

        assertThat(date).isEqualTo(LocalDate.of(2026, 3, 1));
    }
}
