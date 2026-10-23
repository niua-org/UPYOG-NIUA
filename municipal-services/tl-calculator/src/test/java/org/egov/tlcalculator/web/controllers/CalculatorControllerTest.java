// generated-by: generate_per_class_unit_tests.py
package org.egov.tlcalculator.web.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.egov.tlcalculator.service.BPACalculationService;
import org.egov.tlcalculator.service.CalculationService;
import org.egov.tlcalculator.service.DemandService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculatorControllerTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private CalculationService calculationService;
    @Mock
    private DemandService demandService;
    @Mock
    private BPACalculationService bpaCalculationService;

    private CalculatorController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new CalculatorController(objectMapper, request, calculationService, demandService, bpaCalculationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
