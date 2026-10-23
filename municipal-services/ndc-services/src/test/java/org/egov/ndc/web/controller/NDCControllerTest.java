// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.ndc.config.ResponseInfoFactory;
import org.egov.ndc.service.NDCService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NDCControllerTest {
    @Mock
    private ResponseInfoFactory responseInfoFactory;
    @Mock
    private NDCService ndcService;

    private NDCController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new NDCController(responseInfoFactory, ndcService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
