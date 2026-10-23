// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.ndc.config.NDCConfiguration;
import org.egov.ndc.producer.Producer;
import org.egov.ndc.repository.NDCRepository;
import org.egov.ndc.repository.ServiceRequestRepository;
import org.egov.ndc.service.EnrichmentService;
import org.egov.ndc.service.UserService;
import org.egov.ndc.util.NDCUtil;
import org.egov.ndc.workflow.WorkflowIntegrator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NDCServiceTest {
    @Mock
    private NDCUtil ndcUtil;
    @Mock
    private UserService userService;
    @Mock
    private WorkflowIntegrator workflowIntegrator;
    @Mock
    private NDCConfiguration ndcConfiguration;
    @Mock
    private NDCRepository ndcRepository;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private Producer producer;
    @Mock
    private EnrichmentService enrichmentService;

    private NDCService subject;

    @BeforeEach
    void setUp() {
        subject = new NDCService(ndcUtil, userService, workflowIntegrator, ndcConfiguration, ndcRepository, mapper, serviceRequestRepository, producer, enrichmentService);
    }


    @Test
    void createNdcApplication_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
