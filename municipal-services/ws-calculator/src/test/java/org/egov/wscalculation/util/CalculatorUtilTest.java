package org.egov.wscalculation.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.wscalculation.config.WSCalculationConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculatorUtilTest {

    @Mock
    private WSCalculationConfiguration calculationConfig;

    @InjectMocks
    private CalculatorUtil calculatorUtil;

    @Test
    void getMdmsSearchUrl_concatenatesHostAndEndpoint() {
        when(calculationConfig.getMdmsHost()).thenReturn("http://mdms/");
        when(calculationConfig.getMdmsEndPoint()).thenReturn("mdms-v2/_search");

        assertThat(calculatorUtil.getMdmsSearchUrl().toString())
                .isEqualTo("http://mdms/mdms-v2/_search");
    }

    @Test
    void getWaterConnectionModuleRequest_buildsWaterTaxModuleCriteria() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        MdmsCriteriaReq req = calculatorUtil.getWaterConnectionModuleRequest(requestInfo, "pb.amritsar");

        assertThat(req.getRequestInfo()).isSameAs(requestInfo);
        assertThat(req.getMdmsCriteria().getTenantId()).isEqualTo("pb.amritsar");
        assertThat(req.getMdmsCriteria().getModuleDetails()).hasSize(1);
        assertThat(req.getMdmsCriteria().getModuleDetails().get(0).getMasterDetails()).hasSize(6);
    }

    @Test
    void getFinancialYearRequest_filtersAssessmentYearsForWaterModule() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        MdmsCriteriaReq req =
                calculatorUtil.getFinancialYearRequest(requestInfo, Set.of("2023-24"), "pb");

        assertThat(req.getMdmsCriteria().getModuleDetails().get(0).getMasterDetails().get(0).getFilter())
                .contains("2023-24")
                .contains("WS");
    }

    @Test
    void getBillingFrequency_requestsActiveBillingPeriodMaster() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        MdmsCriteriaReq req = calculatorUtil.getBillingFrequency(requestInfo, "pb");

        assertThat(req.getMdmsCriteria().getModuleDetails().get(0).getMasterDetails().get(0).getFilter())
                .contains("active");
    }
}
