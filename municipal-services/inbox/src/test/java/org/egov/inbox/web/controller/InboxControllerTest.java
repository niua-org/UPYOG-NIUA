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
import org.egov.inbox.service.DSSInboxFilterService;
import org.egov.inbox.service.ElasticSearchService;
import org.egov.inbox.service.InboxService;
import org.egov.inbox.util.ResponseInfoFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class InboxControllerTest {
    @Mock
    private InboxService inboxService;
    @Mock
    private ResponseInfoFactory responseInfoFactory;
    @Mock
    private DSSInboxFilterService dssInboxService;
    @Mock
    private ElasticSearchService elasticSearchService;

    @InjectMocks
    private InboxController controller;
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
