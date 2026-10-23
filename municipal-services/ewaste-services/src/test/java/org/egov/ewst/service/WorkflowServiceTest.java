// generated-by: generate_per_class_unit_tests.py
package org.egov.ewst.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.ewst.config.EwasteConfiguration;
import org.egov.ewst.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {
    @Mock
    private EwasteConfiguration configs;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ObjectMapper mapper;

    private WorkflowService subject;

    @BeforeEach
    void setUp() {
        subject = new WorkflowService(configs, serviceRequestRepository, mapper);
    }


    @Test
    void updateWorkflowStatus_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
