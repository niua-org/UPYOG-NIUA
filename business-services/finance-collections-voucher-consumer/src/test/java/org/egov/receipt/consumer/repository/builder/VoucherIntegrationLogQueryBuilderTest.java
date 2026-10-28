package org.egov.receipt.consumer.repository.builder;

import org.egov.receipt.consumer.entity.VoucherIntegrationLog;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class VoucherIntegrationLogQueryBuilderTest {

    @Test
    void getParametersForVoucherIntegartionLog_validLog_mapsAllFields() {
        VoucherIntegrationLog log = new VoucherIntegrationLog();
        log.setReferenceNumber("REF-1");
        log.setStatus("SUCCESS");
        log.setVoucherNumber("V-100");
        log.setType("RECEIPT");
        log.setDescription("ok");
        log.setRequestJson("{\"a\":1}");
        log.setTenantId("pg.citya");
        log.setCreatedDate(new Date(1_700_000_000_000L));

        MapSqlParameterSource params = VoucherIntegrationLogQueryBuilder.getParametersForVoucherIntegartionLog(log);

        assertThat(params.getValue("referencenumber")).isEqualTo("REF-1");
        assertThat(params.getValue("status")).isEqualTo("SUCCESS");
        assertThat(params.getValue("vouchernumber")).isEqualTo("V-100");
        assertThat(params.getValue("tenantid")).isEqualTo("pg.citya");
        assertThat(params.getValue("id")).isNotNull();
    }
}
