package org.upyog.cdwm.calculator.util;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.upyog.cdwm.calculator.web.models.AuditDetails;
import org.upyog.cdwm.calculator.web.models.ResponseInfo;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationUtilsTest {

    @Test
    @DisplayName("getCurrentTimestamp_whenCalled_returnsPositiveValue")
    void getCurrentTimestamp_whenCalled_returnsPositiveValue() {
        assertThat(CalculationUtils.getCurrentTimestamp()).isPositive();
    }

    @Test
    void createReponseInfo_copiesRequestMetadata() {
        RequestInfo requestInfo = RequestInfo.builder().apiId("cnd").ver("1.0").msgId("m1").build();

        ResponseInfo response = CalculationUtils.createReponseInfo(requestInfo, "ok");

        assertThat(response.getApiId()).isEqualTo("cnd");
        assertThat(response.getResMsgId()).isEqualTo("ok");
    }

    @Test
    void parseStringToLocalDate_parsesIsoDate() {
        assertThat(CalculationUtils.parseStringToLocalDate("2024-06-15")).isEqualTo(LocalDate.of(2024, 6, 15));
    }

    @Test
    void getAuditDetails_whenCreate_populatesBothActors() {
        AuditDetails audit = CalculationUtils.getAuditDetails("user", true);

        assertThat(audit.getCreatedBy()).isEqualTo("user");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user");
    }
}
