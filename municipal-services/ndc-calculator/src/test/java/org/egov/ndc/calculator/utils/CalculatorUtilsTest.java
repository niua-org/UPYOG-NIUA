package org.egov.ndc.calculator.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.ndc.calculator.config.NDCCalculatorConfig;
import org.egov.ndc.calculator.web.models.bill.GetBillCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculatorUtilsTest {

    @Mock
    private NDCCalculatorConfig configurations;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private CalculatorUtils calculatorUtils;

    @Test
    void getMdmsSearchUrl_concatenatesHostAndEndpoint() {
        when(configurations.getMdmsHost()).thenReturn("http://mdms");
        when(configurations.getMdmsSearchEndpoint()).thenReturn("/mdms-v2/_search");

        assertThat(calculatorUtils.getMdmsSearchUrl().toString())
                .isEqualTo("http://mdms/mdms-v2/_search");
    }

    @Test
    void getDemandSearchUrl_withConsumerCodes_joinsCodesAndActiveStatus() {
        when(configurations.getBillingHost()).thenReturn("http://billing");
        when(configurations.getDemandSearchEndpoint()).thenReturn("/demand/_search");

        GetBillCriteria criteria = GetBillCriteria.builder()
                .tenantId("pb")
                .consumerCodes(List.of("NDC-1", "NDC-2"))
                .build();

        String url = calculatorUtils.getDemandSearchUrl(criteria).toString();

        assertThat(url).contains("tenantId=pb");
        assertThat(url).contains("NDC-1,NDC-2");
        assertThat(url).contains("ACTIVE");
    }

    @Test
    void getDemandSearchUrl_withoutConsumerCodes_usesApplicantNumber() {
        when(configurations.getBillingHost()).thenReturn("http://billing");
        when(configurations.getDemandSearchEndpoint()).thenReturn("/demand/_search");

        GetBillCriteria criteria =
                GetBillCriteria.builder().tenantId("pb").applicantNumber("APP-9").build();

        String url = calculatorUtils.getDemandSearchUrl(criteria).toString();

        assertThat(url).contains("consumerCode=APP-9");
    }

    @Test
    void getUpdateDemandUrl_usesBillingHostAndUpdateEndpoint() {
        when(configurations.getBillingHost()).thenReturn("http://billing");
        when(configurations.getDemandUpdateEndpoint()).thenReturn("/demand/_update");

        assertThat(calculatorUtils.getUpdateDemandUrl().toString())
                .isEqualTo("http://billing/demand/_update");
    }
}
