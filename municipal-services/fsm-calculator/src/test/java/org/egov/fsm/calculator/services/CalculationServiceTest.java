package org.egov.fsm.calculator.services;

import org.egov.fsm.calculator.web.models.CalculationReq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

    @Mock private MDMSService mdmsService;
    @Mock private DemandService demandService;
    @Mock private org.egov.fsm.calculator.config.CalculatorConfig config;
    @Mock private org.egov.fsm.calculator.utils.CalculationUtils utils;
    @Mock private org.egov.fsm.calculator.kafka.broker.CalculatorProducer producer;
    @Mock private FSMService fsmService;
    @Mock private org.egov.fsm.calculator.repository.querybuilder.BillingSlabQueryBuilder billingSlabQueryBuilder;
    @Mock private org.egov.fsm.calculator.repository.BillingSlabRepository billingSlabRepository;
    @Mock private BillingService billingService;

    @InjectMocks
    private CalculationService calculationService;

    @Test
    void calculate_whenCriteriaEmpty_throws() {
        CalculationReq req = new CalculationReq();
        req.setCalulationCriteria(Collections.emptyList());

        assertThatThrownBy(() -> calculationService.calculate(req))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }
}
