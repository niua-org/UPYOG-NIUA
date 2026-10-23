// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.workflow.WorkflowConfig;
import org.egov.tl.workflow.WorkflowService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ActionValidatorTest {
    @Mock
    private WorkflowConfig workflowConfig;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private TLConfiguration config;

    private ActionValidator subject;

    @BeforeEach
    void setUp() {
        subject = new ActionValidator(workflowConfig, workflowService, config);
    }


    @Test
    void validateCreateRequest_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
