package org.egov.tlcalculator.utils;

import org.egov.tlcalculator.config.TLCalculatorConfigs;
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
    private TLCalculatorConfigs config;

    @InjectMocks
    private CalculationUtils calculationUtils;

    @Test
    void getDemandSearchURL_buildsDemandSearchTemplate() {
        when(config.getBillingHost()).thenReturn("http://billing");
        when(config.getDemandSearchEndpoint()).thenReturn("/demand/_search");

        assertThat(calculationUtils.getDemandSearchURL()).contains("http://billing/demand/_search");
    }

    @Test
    void getBillGenerateURI_containsBusinessServicePlaceholder() {
        when(config.getBillingHost()).thenReturn("http://billing");
        when(config.getBillGenerateEndpoint()).thenReturn("/bill/_generate");

        assertThat(calculationUtils.getBillGenerateURI()).contains("businessService={3}");
    }
}
