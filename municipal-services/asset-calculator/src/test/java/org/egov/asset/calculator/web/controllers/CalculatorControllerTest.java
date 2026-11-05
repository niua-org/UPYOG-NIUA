// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.calculator.web.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.asset.calculator.services.CalculationService;
import org.egov.asset.calculator.services.ProcessDepreciation;
import org.egov.asset.calculator.utils.ResponseInfoFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculatorControllerTest {
    @Mock
    private ProcessDepreciation depreciationService;
    @Mock
    private CalculationService calculationService;
    @Mock
    private ResponseInfoFactory responseInfoFactory;

    private CalculatorController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new CalculatorController(depreciationService, calculationService, responseInfoFactory);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
