// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.ndc.service.EnrichmentService;
import org.egov.ndc.service.NDCService;
import org.egov.ndc.workflow.WorkflowIntegrator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentUpdateServiceTest {
    @Mock
    private NDCService ndcService;
    @Mock
    private WorkflowIntegrator wfIntegrator;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private ObjectMapper mapper;

    private PaymentUpdateService subject;

    @BeforeEach
    void setUp() {
        subject = new PaymentUpdateService(ndcService, wfIntegrator, enrichmentService, mapper);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
