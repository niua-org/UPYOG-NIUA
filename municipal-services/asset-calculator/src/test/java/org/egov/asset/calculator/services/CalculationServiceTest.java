package org.egov.asset.calculator.services;

import org.egov.asset.calculator.web.models.CalulationCriteria;
import org.egov.asset.calculator.web.models.CalculationReq;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

    @Mock private ProcessDepreciationV2 depreciationServiceV2;
    @InjectMocks private CalculationService calculationService;

    @Test
    void calculate_delegatesToDepreciationService() {
        CalculationReq req = CalculationReq.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().uuid("u1").build()).build())
                .calulationCriteria(CalulationCriteria.builder().tenantId("pb.amritsar").assetId("a1").build())
                .build();
        when(depreciationServiceV2.calculateDepreciation("pb.amritsar", "a1", false, "u1")).thenReturn("ok");

        assertThat(calculationService.calculate(req).getMessage()).isEqualTo("ok");
    }
}
