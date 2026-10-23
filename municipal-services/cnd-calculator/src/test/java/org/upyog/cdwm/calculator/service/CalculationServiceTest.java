package org.upyog.cdwm.calculator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.cdwm.calculator.util.MdmsUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {
    @Mock
    private MdmsUtil mdmsUtil;

    private CalculationService subject;

    @BeforeEach
    void setUp() {
        subject = new CalculationService(mdmsUtil);
    }


    @Test
    void calculateFee_whenMdmsEmpty_throwsCustomException() {
        org.egov.common.contract.request.RequestInfo requestInfo =
                new org.egov.common.contract.request.RequestInfo();
        org.upyog.cdwm.calculator.web.models.CNDApplicationDetail detail =
                new org.upyog.cdwm.calculator.web.models.CNDApplicationDetail();
        detail.setTenantId("pb.amritsar");
        detail.setTotalWasteQuantity(java.math.BigDecimal.ONE);
        org.mockito.Mockito.when(mdmsUtil.getCalculationType(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.Collections.emptyList());
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> subject.calculateFee(detail, requestInfo))
                .isInstanceOf(org.egov.tracer.model.CustomException.class);
    }

    @Test
    void calculateFee_whenMdmsReturnsAmount_multipliesQuantity() {
        org.egov.common.contract.request.RequestInfo requestInfo =
                new org.egov.common.contract.request.RequestInfo();
        org.upyog.cdwm.calculator.web.models.CNDApplicationDetail detail =
                new org.upyog.cdwm.calculator.web.models.CNDApplicationDetail();
        detail.setTenantId("pb.amritsar");
        detail.setTotalWasteQuantity(new java.math.BigDecimal("2"));
        org.upyog.cdwm.calculator.web.models.CalculationType type =
                new org.upyog.cdwm.calculator.web.models.CalculationType();
        type.setAmount(new java.math.BigDecimal("10"));
        org.mockito.Mockito.when(mdmsUtil.getCalculationType(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.List.of(type));
        org.assertj.core.api.Assertions.assertThat(subject.calculateFee(detail, requestInfo))
                .isEqualByComparingTo(new java.math.BigDecimal("20"));
    }

}
