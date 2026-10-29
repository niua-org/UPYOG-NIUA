package org.upyog.tp.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.tp.util.MdmsUtil;

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
        when(mdmsUtil.getCalculationType(any(), any(), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> calculationService.calculateFee(RequestInfo.builder().build(), "pb.amritsar"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Fee not found");
    }
}
