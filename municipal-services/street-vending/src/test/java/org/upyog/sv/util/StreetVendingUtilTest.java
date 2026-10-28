package org.upyog.sv.util;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.Test;
import org.upyog.sv.web.models.common.AuditDetails;
import org.upyog.sv.web.models.common.ResponseInfo;
import org.upyog.sv.web.models.common.ResponseInfo.StatusEnum;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StreetVendingUtilTest {

    @Test
    void createReponseInfo_whenRequestPresent_copiesMetadata() {
        RequestInfo requestInfo = RequestInfo.builder().apiId("sv").ver("1.0").msgId("msg-1").build();

        ResponseInfo response = StreetVendingUtil.createReponseInfo(requestInfo, "res-1", StatusEnum.SUCCESSFUL);

        assertThat(response.getApiId()).isEqualTo("sv");
        assertThat(response.getStatus()).isEqualTo(StatusEnum.SUCCESSFUL);
    }

    @Test
    void getAuditDetails_whenCreate_populatesCreatedAndModified() {
        AuditDetails audit = StreetVendingUtil.getAuditDetails("user-1", true);

        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-1");
    }

    @Test
    void parseStringToLocalDate_parsesConfiguredFormat() {
        LocalDate date = StreetVendingUtil.parseStringToLocalDate("2026-04-10");

        assertThat(date).isEqualTo(LocalDate.of(2026, 4, 10));
    }
}
