// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.calculator.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.pt.calculator.service.DemandService;
import org.egov.pt.calculator.service.EstimationService;
import org.egov.pt.calculator.service.PayService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculatorControllerTest {
    @Mock
    private DemandService demandService;
    @Mock
    private EstimationService calculatorService;
    @Mock
    private PayService payService;

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
