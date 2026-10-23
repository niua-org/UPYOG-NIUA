// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.anomaly.finder.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.pqm.anomaly.finder.repository.AnomalyRepository;
import org.egov.pqm.anomaly.finder.service.AnomalyFinderService;
import org.egov.pqm.anomaly.finder.util.ResponseInfoFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PqmAnomalyFinderControllerTest {
    @Mock
    private AnomalyRepository anomalyRepository;
    @Mock
    private AnomalyFinderService anomalyFinderService;
    @Mock
    private ResponseInfoFactory responseInfoFactory;

    @InjectMocks
    private PqmAnomalyFinderController controller;
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
