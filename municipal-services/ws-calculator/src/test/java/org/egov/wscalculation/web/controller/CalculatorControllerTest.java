// generated-by: generate_per_class_unit_tests.py
package org.egov.wscalculation.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.wscalculation.config.WSCalculationConfiguration;
import org.egov.wscalculation.service.DemandNotificationService;
import org.egov.wscalculation.service.DemandService;
import org.egov.wscalculation.service.PaymentNotificationService;
import org.egov.wscalculation.service.WSCalculationService;
import org.egov.wscalculation.service.WSCalculationServiceImpl;
import org.egov.wscalculation.util.ResponseInfoFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculatorControllerTest {
    @Mock
    private DemandService demandService;
    @Mock
    private WSCalculationService wSCalculationService;
    @Mock
    private WSCalculationServiceImpl wSCalculationServiceImpl;
    @Mock
    private ResponseInfoFactory responseInfoFactory;
    @Mock
    private PaymentNotificationService paymentNotificationService;
    @Mock
    private WSCalculationConfiguration config;
    @Mock
    private DemandNotificationService demandNotificationService;

    @InjectMocks
    private CalculatorController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
