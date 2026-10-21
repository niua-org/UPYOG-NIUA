// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.pqm.config.ServiceConfiguration;
import org.egov.pqm.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {
    @Mock
    private ServiceConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ObjectMapper mapper;

    private WorkflowService subject;

    @BeforeEach
    void setUp() {
        subject = new WorkflowService(config, serviceRequestRepository, mapper);
    }


    @Test
    void getBusinessService_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
