package org.egov.fsm.calculator.utils;

import org.egov.fsm.calculator.config.CalculatorConfig;
import org.egov.fsm.calculator.web.models.AuditDetails;
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
    private CalculatorConfig config;

    @InjectMocks
    private CalculationUtils calculationUtils;

    @Test
    void getDemandSearchURL_usesBillingHostAndEndpoint() {
        when(config.getBillingHost()).thenReturn("http://billing");
        when(config.getDemandSearchEndpoint()).thenReturn("/demand/_search");

        assertThat(calculationUtils.getDemandSearchURL()).contains("http://billing/demand/_search");
    }

    @Test
    void getBillGenerateURI_containsConsumerPlaceholder() {
        when(config.getBillingHost()).thenReturn("http://billing");
        when(config.getBillGenerateEndpoint()).thenReturn("/bill/_generate");

        assertThat(calculationUtils.getBillGenerateURI()).contains("consumerCode={2}");
    }

    @Test
    void getAuditDetails_whenCreate_setsCreatedBy() {
        AuditDetails audit = calculationUtils.getAuditDetails("fsm-user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("fsm-user");
    }
}
