package org.egov.echallancalculation.util;

import org.egov.echallancalculation.config.ChallanConfiguration;
import org.egov.echallancalculation.model.AuditDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationUtilsTest {

    @Mock
    private ChallanConfiguration config;

    @InjectMocks
    private CalculationUtils calculationUtils;

    @Test
    void getDemandSearchURL_buildsBillingDemandTemplate() {
        when(config.getBillingHost()).thenReturn("http://billing");
        when(config.getDemandSearchEndpoint()).thenReturn("/demand/_search");

        assertThat(calculationUtils.getDemandSearchURL()).contains("http://billing/demand/_search");
    }

    @Test
    void getAuditDetails_whenUpdate_onlyLastModifiedFields() {
        AuditDetails audit = calculationUtils.getAuditDetails("ops", false);

        assertThat(audit.getCreatedBy()).isNull();
        assertThat(audit.getLastModifiedBy()).isEqualTo("ops");
    }
}
