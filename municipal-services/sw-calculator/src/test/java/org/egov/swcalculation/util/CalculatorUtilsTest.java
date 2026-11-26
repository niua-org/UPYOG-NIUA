package org.egov.swcalculation.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.swcalculation.config.SWCalculationConfiguration;
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
    private SWCalculationConfiguration configurations;

    @InjectMocks
    private CalculatorUtils calculatorUtils;

    @Test
    void getMdmsSearchUrl_concatenatesHostAndEndpoint() {
        when(configurations.getMdmsHost()).thenReturn("http://mdms/");
        when(configurations.getMdmsEndPoint()).thenReturn("_search");

        assertThat(calculatorUtils.getMdmsSearchUrl().toString()).isEqualTo("http://mdms/_search");
    }

    @Test
    void getFinancialYearRequest_buildsSewerageFinancialYearFilter() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        MdmsCriteriaReq req =
                calculatorUtils.getFinancialYearRequest(requestInfo, Set.of("2024-25"), "pb");

        assertThat(req.getMdmsCriteria().getTenantId()).isEqualTo("pb");
        assertThat(req.getMdmsCriteria().getModuleDetails().get(0).getMasterDetails().get(0).getFilter())
                .contains("2024-25")
                .contains("SW");
    }
}
