package org.egov.bpa.calculator.utils;

import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.web.models.AuditDetails;
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
    private BPACalculatorConfig config;

    @InjectMocks
    private CalculationUtils calculationUtils;

    @Test
    void getDemandSearchURL_buildsPlaceholderTemplate() {
        when(config.getBillingHost()).thenReturn("http://billing");
        when(config.getDemandSearchEndpoint()).thenReturn("/demand/_search");

        assertThat(calculationUtils.getDemandSearchURL())
                .contains("tenantId={1}")
                .contains("businessService={2}")
                .contains("consumerCode={3}");
    }

    @Test
    void getAuditDetails_whenCreate_populatesCreatedFields() {
        AuditDetails audit = calculationUtils.getAuditDetails("user-1", true);

        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-1");
        assertThat(audit.getCreatedTime()).isNotNull();
    }

    @Test
    void getAuditDetails_whenUpdate_onlyLastModified() {
        AuditDetails audit = calculationUtils.getAuditDetails("user-2", false);

        assertThat(audit.getCreatedBy()).isNull();
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-2");
    }
}
