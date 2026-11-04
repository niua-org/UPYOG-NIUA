// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.calculator.web.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.ndc.calculator.services.CalculationService;
import org.egov.ndc.calculator.services.DemandService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NDCCalculatorControllerTest {
    @Mock
    private CalculationService calculationService;
    @Mock
    private DemandService demandService;

    private NDCCalculatorController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new NDCCalculatorController(calculationService, demandService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
