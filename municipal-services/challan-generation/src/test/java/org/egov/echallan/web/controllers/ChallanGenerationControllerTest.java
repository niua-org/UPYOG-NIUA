// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.web.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.echallan.producer.Producer;
import org.egov.echallan.service.ChallanService;
import org.egov.echallan.util.ResponseInfoFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChallanGenerationControllerTest {
    @Mock
    private ChallanService challanService;
    @Mock
    private ResponseInfoFactory responseInfoFactory;
    @Mock
    private Producer producer;

    private ChallanGenerationController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new ChallanGenerationController(challanService, responseInfoFactory, producer);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
