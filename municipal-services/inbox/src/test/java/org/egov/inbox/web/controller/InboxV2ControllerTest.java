// generated-by: generate_per_class_unit_tests.py
package org.egov.inbox.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.inbox.repository.builder.V2.InboxQueryBuilder;
import org.egov.inbox.service.V2.InboxServiceV2;
import org.egov.inbox.util.ResponseInfoFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class InboxV2ControllerTest {
    @Mock
    private ResponseInfoFactory responseInfoFactory;
    @Mock
    private InboxQueryBuilder inboxQueryBuilder;
    @Mock
    private InboxServiceV2 inboxService;

    @InjectMocks
    private InboxV2Controller controller;
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
