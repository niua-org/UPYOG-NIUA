package org.upyog.cdwm.calculator.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.cdwm.calculator.util.MdmsUtil;
import org.upyog.cdwm.calculator.web.models.CNDApplicationDetail;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

    @Mock private MdmsUtil mdmsUtil;
    @InjectMocks private CalculationService calculationService;

    @Test
    void calculateFee_whenMdmsEmpty_throws() {
        CNDApplicationDetail detail = CNDApplicationDetail.builder()
                .tenantId("pb.amritsar")
                .totalWasteQuantity(BigDecimal.ONE)
                .build();
        when(mdmsUtil.getCalculationType(any(), any(), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> calculationService.calculateFee(detail, RequestInfo.builder().build()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Fee not found");
    }
}
