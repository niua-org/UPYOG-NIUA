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
import org.egov.pt.calculator.service.EstimationService;
import org.egov.pt.calculator.service.TranslationService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculationV2ControllerTest {
    @Mock
    private TranslationService translationService;
    @Mock
    private EstimationService estimationService;

    private CalculationV2Controller controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new CalculationV2Controller(translationService, estimationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
