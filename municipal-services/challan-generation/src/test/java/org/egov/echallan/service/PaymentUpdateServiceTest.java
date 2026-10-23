// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.producer.Producer;
import org.egov.echallan.service.ChallanService;
import org.egov.echallan.util.CommonUtils;
import org.egov.echallan.workflow.WorkflowIntegrator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentUpdateServiceTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private ChallanService challanService;
    @Mock
    private Producer producer;
    @Mock
    private ChallanConfiguration config;
    @Mock
    private CommonUtils commUtils;
    @Mock
    private WorkflowIntegrator workflowIntegrator;

    private PaymentUpdateService subject;

    @BeforeEach
    void setUp() {
        subject = new PaymentUpdateService(mapper, challanService, producer, config, commUtils, workflowIntegrator);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
