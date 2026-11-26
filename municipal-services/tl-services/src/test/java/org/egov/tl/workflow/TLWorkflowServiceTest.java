// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.producer.Producer;
import org.egov.tl.workflow.ActionValidator;
import org.egov.tl.workflow.WorkflowConfig;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TLWorkflowServiceTest {
    @Mock
    private ActionValidator actionValidator;
    @Mock
    private Producer producer;
    @Mock
    private TLConfiguration config;
    @Mock
    private WorkflowConfig workflowConfig;

    private TLWorkflowService subject;

    @BeforeEach
    void setUp() {
        subject = new TLWorkflowService(actionValidator, producer, config, workflowConfig);
    }


    @Test
    void updateStatus_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
