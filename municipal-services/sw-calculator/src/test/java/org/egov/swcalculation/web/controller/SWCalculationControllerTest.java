// generated-by: generate_per_class_unit_tests.py
package org.egov.swcalculation.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.swcalculation.service.DemandService;
import org.egov.swcalculation.service.SWCalculationService;
import org.egov.swcalculation.service.SWCalculationServiceImpl;
import org.egov.swcalculation.util.ResponseInfoFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SWCalculationControllerTest {
    @Mock
    private SWCalculationService sWCalculationService;
    @Mock
    private DemandService demandService;
    @Mock
    private ResponseInfoFactory responseInfoFactory;
    @Mock
    private SWCalculationServiceImpl sWCalculationServiceImpl;

    @InjectMocks
    private SWCalculationController controller;
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
