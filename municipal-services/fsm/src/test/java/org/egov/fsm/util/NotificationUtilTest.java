// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.fsm.config.FSMConfiguration;
import org.egov.fsm.fsmProducer.FSMProducer;
import org.egov.fsm.repository.ServiceRequestRepository;
import org.egov.fsm.service.DSOService;
import org.egov.fsm.workflow.WorkflowService;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationUtilTest {
    @Mock
    private FSMConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private FSMProducer producer;
    @Mock
    private DSOService dsoService;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private WorkflowService workflowService;

    private NotificationUtil subject;

    @BeforeEach
    void setUp() {
        subject = new NotificationUtil(config, serviceRequestRepository, producer, dsoService, restTemplate, workflowService);
    }


    @Test
    void getCustomizedMsg_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
