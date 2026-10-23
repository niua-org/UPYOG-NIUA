// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.upyog.sv.config.StreetVendingConfiguration;
import org.upyog.sv.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {
    @Mock
    private StreetVendingConfiguration configs;
    @Mock
    private ServiceRequestRepository restRepo;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private WorkflowService subject;

    @BeforeEach
    void setUp() {
        subject = new WorkflowService(configs, restRepo, mapper, serviceRequestRepository);
    }


    @Test
    void updateWorkflowStatus_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
