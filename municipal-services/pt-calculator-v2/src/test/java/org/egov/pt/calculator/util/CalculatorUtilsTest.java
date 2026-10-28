package org.egov.pt.calculator.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.pt.calculator.util.Configurations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculatorUtilsTest {

    @Mock
    private Configurations configurations;

    @InjectMocks
    private CalculatorUtils calculatorUtils;

    @Test
    void getMdmsSearchUrl_concatenatesHostAndEndpoint() {
        when(configurations.getMdmsHost()).thenReturn("http://mdms/");
        when(configurations.getMdmsEndpoint()).thenReturn("mdms-v2/_search");

        assertThat(calculatorUtils.getMdmsSearchUrl().toString())
                .isEqualTo("http://mdms/mdms-v2/_search");
    }

    @Test
    void getFinancialYearRequest_buildsPtModuleFilter() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        MdmsCriteriaReq req =
                calculatorUtils.getFinancialYearRequest(requestInfo, Set.of("2022-23"), "pb");

        assertThat(req.getMdmsCriteria().getTenantId()).isEqualTo("pb");
        assertThat(req.getMdmsCriteria().getModuleDetails().get(0).getMasterDetails().get(0).getFilter())
                .contains("2022-23")
                .contains("PT");
    }
}
