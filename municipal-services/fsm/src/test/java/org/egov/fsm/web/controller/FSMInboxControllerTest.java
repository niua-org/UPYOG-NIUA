// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.fsm.service.FSMInboxService;
import org.egov.fsm.util.ResponseInfoFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FSMInboxControllerTest {
    @Mock
    private FSMInboxService fsmInboxService;
    @Mock
    private ResponseInfoFactory responseInfoFactory;

    @InjectMocks
    private FSMInboxController controller;
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
