package org.upyog.rs.util;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.Test;
import org.upyog.rs.web.models.AuditDetails;
import org.upyog.rs.web.models.ResponseInfo.StatusEnum;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequestServiceUtilTest {

    @Test
    void createReponseInfo_copiesRequestMetadata() {
        RequestInfo requestInfo = RequestInfo.builder().apiId("rs").ver("1.0").msgId("m1").build();

        var response = RequestServiceUtil.createReponseInfo(requestInfo, "res-1", StatusEnum.SUCCESSFUL);

        assertThat(response.getApiId()).isEqualTo("rs");
        assertThat(response.getStatus()).isEqualTo(StatusEnum.SUCCESSFUL);
    }

    @Test
    void getAuditDetails_whenCreate_populatesCreatedFields() {
        AuditDetails audit = RequestServiceUtil.getAuditDetails("user-1", true);

        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-1");
        assertThat(audit.getCreatedTime()).isNotNull();
    }

    @Test
    void parseStringToLocalDate_parsesIsoDate() {
        assertThat(RequestServiceUtil.parseStringToLocalDate("2026-05-01"))
                .isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void isDateWithinRange_whenInsideRange_returnsTrue() {
        assertThat(RequestServiceUtil.isDateWithinRange("2026-01-01", "2026-01-31", "2026-01-15")).isTrue();
    }

    @Test
    void isDateRangeOverlap_whenRangesTouch_returnsTrue() {
        assertThat(RequestServiceUtil.isDateRangeOverlap("2026-02-01", "2026-02-10", "2026-02-10", "2026-02-20"))
                .isTrue();
    }

    @Test
    void getTenantId_returnsStateLevelPrefix() {
        assertThat(RequestServiceUtil.getTenantId("pb.amritsar")).isEqualTo("pb");
    }

    @Test
    void beuatifyJson_serializesObject() {
        String json = RequestServiceUtil.beuatifyJson(Map.of("key", "value"));

        assertThat(json).contains("\"key\"");
    }
}
